package com.github.jcustenborder.kafka.connect.redis;

import com.github.jcustenborder.docker.junit5.Compose;
import com.github.jcustenborder.kafka.connect.redis.healthchecks.RedisStandardHealthCheck;
import com.palantir.docker.compose.connection.Cluster;

@Compose(
    dockerComposePath = "src/test/resources/docker/standard/docker-compose.yml",
    clusterHealthCheck = RedisStandardHealthCheck.class
)
public class StandardRedisCacheSinkTaskIT extends AbstractTaskRedisCacheSinkTaskIT {
  @Override
  protected ConnectionHelper createConnectionHelper(Cluster cluster) {
    return new ConnectionHelper.Standard(cluster);
  }
}