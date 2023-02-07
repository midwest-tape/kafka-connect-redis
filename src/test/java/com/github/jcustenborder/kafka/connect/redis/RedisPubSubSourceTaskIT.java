/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.redis;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.jcustenborder.kafka.connect.redis.TestUtils.assertHeader;
import static com.github.jcustenborder.kafka.connect.redis.TestUtils.assertRecords;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class RedisPubSubSourceTaskIT extends AbstractSourceTaskIntegrationTest<RedisPubSubSourceTask> {
  private static final Logger log = LoggerFactory.getLogger(RedisPubSubSourceTaskIT.class);

  @Override
  protected RedisPubSubSourceTask createTask() {
    return new RedisPubSubSourceTask();
  }

  List<SourceRecord> waitForRecords(final int expectedCount) throws InterruptedException {
    List<SourceRecord> result = new ArrayList<>(expectedCount);

    while (result.size() < expectedCount) {
      List<SourceRecord> poll = this.task.poll();
      if (null != poll) {
        result.addAll(poll);
        log.info("adding {} records", poll.size());
      }
    }

    return result;
  }

  @Test
  public void channelSubscribe() throws Exception {

    final String channelName = "channelSubscribeTest";
    final byte[] channelBytes = channelName.getBytes(StandardCharsets.UTF_8);

    SourceTaskContext context = mock(SourceTaskContext.class);
    OffsetStorageReader offsetStorageReader = mock(OffsetStorageReader.class);
    when(context.offsetStorageReader()).thenReturn(offsetStorageReader);
    this.task.initialize(context);
    this.settings.put(RedisPubSubSourceConnectorConfig.REDIS_CHANNELS_CONF, channelName);
    this.task.start(this.settings);
    final List<byte[]> input = TestLocation.loadLocations().stream()
        .map(TestLocation::ident)
        .map(s -> s.getBytes(this.task.config.charset))
        .collect(Collectors.toList());

    try (RedisPubSubSession session = this.task.sessionFactory.createPubSubSession(this.task.config)) {
      log.info("Publishing {} message(s) to redis channel '{}'.", input.size(), channelName);
      boolean success = LettuceFutures.awaitAll(30, TimeUnit.SECONDS, input.stream()
          .map(l -> session.asyncCommands().publish(channelBytes, l))
          .toArray(RedisFuture[]::new));
      assertTrue(success);
      assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
        List<SourceRecord> records = waitForRecords(input.size());
        assertRecords(input, records, (expected, record) -> {
          assertHeader(record, "redis.channel", channelName);
        });
      });
    }
  }

  @Test
  public void patternSubscribe() throws Exception {
    final List<String> channels = IntStream.range(1, 10).boxed()
        .map(i -> String.format("patternSubscribe%s", i))
        .collect(Collectors.toList());
    SourceTaskContext context = mock(SourceTaskContext.class);
    OffsetStorageReader offsetStorageReader = mock(OffsetStorageReader.class);
    when(context.offsetStorageReader()).thenReturn(offsetStorageReader);
    this.task.initialize(context);
    this.settings.put(RedisPubSubSourceConnectorConfig.REDIS_CHANNEL_PATTERNS_CONF, "patternSubscribe*");
    this.task.start(this.settings);
    final List<byte[]> input = TestLocation.loadLocations().stream()
        .map(TestLocation::ident)
        .map(s -> s.getBytes(this.task.config.charset))
        .collect(Collectors.toList());
    final int expectedRecords = channels.size() * input.size();

    try (RedisPubSubSession session = this.task.sessionFactory.createPubSubSession(this.task.config)) {
      List<RedisFuture<?>> futures = new ArrayList<>(expectedRecords);
      input.forEach(v -> {
        channels.stream().map(channel -> session.asyncCommands()
                .publish(channel.getBytes(StandardCharsets.UTF_8), v))
            .forEach(futures::add);
      });

      LettuceFutures.awaitAll(30, TimeUnit.SECONDS, futures.toArray(new RedisFuture[0]));
    }
    assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
      List<SourceRecord> records = waitForRecords(expectedRecords);
      Map<String, List<SourceRecord>> recordsByChannel = new LinkedHashMap<>();
      for (SourceRecord record : records) {
        Header header = record.headers().lastWithName("redis.channel");
        String headerValue = new String((byte[]) header.value(), StandardCharsets.UTF_8);
        List<SourceRecord> sourceRecords = recordsByChannel.getOrDefault(headerValue, new ArrayList<>());
        sourceRecords.add(record);
      }

      recordsByChannel.forEach((channelName, sourceRecords) -> {
        assertRecords(input, sourceRecords, (expected, record) -> {
          assertHeader(record, "redis.channel", channelName);
        });
      });
    });
  }


}