package com.github.jcustenborder.kafka.connect.redis.cluster;

import com.github.jcustenborder.docker.junit5.Compose;
import com.github.jcustenborder.kafka.connect.redis.ConnectionHelper;
import com.github.jcustenborder.kafka.connect.redis.RedisPubSubSourceTaskIT;
import com.github.jcustenborder.kafka.connect.redis.healthchecks.RedisClusterHealthCheck;
import com.palantir.docker.compose.connection.Cluster;
import org.junit.jupiter.api.Disabled;

@Compose(
    dockerComposePath = "src/test/resources/docker/cluster/docker-compose.yml",
    clusterHealthCheck = RedisClusterHealthCheck.class
)
public class RedisPubSubSourceTaskClusterIT extends RedisPubSubSourceTaskIT {
  @Override
  protected ConnectionHelper createConnectionHelper(Cluster cluster) {
    return new ConnectionHelper.RedisCluster(cluster);
  }
}