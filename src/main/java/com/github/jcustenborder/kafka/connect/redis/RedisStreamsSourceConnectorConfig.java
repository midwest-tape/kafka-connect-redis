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

import com.github.jcustenborder.kafka.connect.utils.config.ConfigKeyBuilder;
import com.github.jcustenborder.kafka.connect.utils.config.ConfigUtils;
import io.lettuce.core.Consumer;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;
import java.util.Set;

class RedisStreamsSourceConnectorConfig extends RedisConnectorConfig {
  public final static String REDIS_STREAMS_CONF = "redis.streams";
  public final static String REDIS_CONSUMER_GROUP_CONF = "redis.consumer.group";
  public final static String REDIS_CONSUMER_ID_CONF = "redis.consumer.id";
  final static String REDIS_STREAMS_DOC = "The Redis stream(s) the connector should read from.";
  final static String REDIS_CONSUMER_GROUP_DOC = "The consumer group to read the Redis Stream with.";
  final static String REDIS_CONSUMER_ID_DOC = "The consumer id for the individual task. This is generated by the connector.";

  public final Set<String> streams;
  public final String consumerGroup;
  public final int consumerId;

  public RedisStreamsSourceConnectorConfig(Map<?, ?> originals) {
    super(config(), originals);
    this.streams = ConfigUtils.getSet(this, REDIS_STREAMS_CONF);
    this.consumerGroup = getString(REDIS_CONSUMER_GROUP_CONF);
    this.consumerId = getInt(REDIS_CONSUMER_ID_CONF);
  }

  public static ConfigDef config() {
    return RedisConnectorConfig.config()
        .define(
            ConfigKeyBuilder.of(REDIS_STREAMS_CONF, ConfigDef.Type.LIST)
                .documentation(REDIS_STREAMS_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .build()
        )
        .define(
            ConfigKeyBuilder.of(REDIS_CONSUMER_GROUP_CONF, ConfigDef.Type.STRING)
                .documentation(REDIS_CONSUMER_GROUP_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .build()
        )
        .define(
            ConfigKeyBuilder.of(REDIS_CONSUMER_ID_CONF, ConfigDef.Type.INT)
                .documentation(REDIS_CONSUMER_ID_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue(-1)
                .build()
        );
  }

  public Consumer<String> consumer() {
    String consumerName = String.format(
        "%s-%s",
        RedisStreamsSinkTask.class.getSimpleName(),
        this.consumerId
    );
    return Consumer.from(
        this.consumerGroup,
        consumerName
    );
  }
}
