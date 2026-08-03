package com.freshlink.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;

/**
 * Wiring for Redis-backed rate limiting.
 *
 * Bucket4j needs a byte[]/byte[] Lettuce connection, which is not the codec
 * Spring's own connection factory uses, so it gets a dedicated client rather
 * than sharing one.
 *
 * Only created when {@code app.ratelimit.backend=redis}, so a single-instance
 * deployment never opens a Redis connection at all.
 */
@Configuration
@ConditionalOnProperty(name = "app.ratelimit.backend", havingValue = "redis")
public class RedisConfig {

	@Value("${spring.data.redis.host:localhost}")
	private String host;

	@Value("${spring.data.redis.port:6379}")
	private int port;

	@Bean(destroyMethod = "shutdown")
	public RedisClient bucketRedisClient() {
		return RedisClient.create(RedisURI.builder().withHost(host).withPort(port).build());
	}

	@Bean(destroyMethod = "close")
	public StatefulRedisConnection<byte[], byte[]> bucketRedisConnection(RedisClient client) {
		return client.connect(ByteArrayCodec.INSTANCE);
	}

	@Bean
	public ProxyManager<byte[]> bucketProxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
		return LettuceBasedProxyManager.builderFor(connection)
				// Buckets expire once they would have fully refilled, so idle keys
				// clean themselves up instead of accumulating one per IP forever.
				.withExpirationStrategy(ExpirationAfterWriteStrategy
						.basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(1)))
				.build();
	}
}
