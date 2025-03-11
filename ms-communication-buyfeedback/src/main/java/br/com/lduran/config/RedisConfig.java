package br.com.lduran.config;

import br.com.lduran.domain.entity.CompraRedis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig
{
	@Value("${spring.redis.host:redis}")
	private String redisHost;

	@Value("${spring.redis.port:6390}")
	private int redisPort;

	@Bean
	public LettuceConnectionFactory redisConnectionFactory()
	{
		RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration("redis", 6379);
		return new LettuceConnectionFactory(redisConfig);
	}

	@Bean
	public RedisTemplate<String, CompraRedis> redisTemplate()
	{
		RedisTemplate<String, CompraRedis> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory());
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		return template;
	}
}