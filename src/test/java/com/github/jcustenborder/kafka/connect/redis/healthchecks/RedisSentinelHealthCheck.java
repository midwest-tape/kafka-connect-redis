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
package com.github.jcustenborder.kafka.connect.redis.healthchecks;

import com.github.jcustenborder.kafka.connect.redis.RedisConfigHelper;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisSentinelHealthCheck extends AbstractRedisHealthCheck {
  private static final Logger log = LoggerFactory.getLogger(RedisSentinelHealthCheck.class);

  @Override
  public SuccessOrFailure isClusterHealthy(Cluster cluster) throws InterruptedException {
    return SuccessOrFailure.onResultOf(() -> {

      SuccessOrFailure allPortsOpen = cluster.container("redis").areAllPortsOpen();
      if (allPortsOpen.failed()) {
        return allPortsOpen.failed();
      }

      RedisURI redisURI = RedisConfigHelper.sentenielURI();
      log.info("Connecting to {}", redisURI);

      try (RedisClient client = RedisClient.create(redisURI)) {
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
          RedisCommands<String, String> syncCommands = connection.sync();
          String info = syncCommands.info();
          log.info(info);
          return testKeys(syncCommands);
        }
      }
    });
  }
}
