package br.com.lduran.domain.service;

import br.com.lduran.domain.entity.CompraRedis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RedisService
{
	private RedisTemplate<String, CompraRedis> redisTemplate;
	private final HashOperations<String, String, CompraRedis> hashOperations;

	@Autowired
	public RedisService(RedisTemplate<String, CompraRedis> redisTemplate)
	{
		this.redisTemplate = redisTemplate;
		this.hashOperations = redisTemplate.opsForHash();
	}

	public void salvar(String chave, CompraRedis compraRedis)
	{
		hashOperations.put("CompraRedis", chave, compraRedis);
	}

	public Optional<CompraRedis> buscar(String chave)
	{
		return Optional.ofNullable(hashOperations.get("CompraRedis", chave));
	}

	public List<CompraRedis> findAll()
	{
		Map<String, CompraRedis> entries = hashOperations.entries("CompraRedis");
		List<CompraRedis> compras = new ArrayList<>();
		for (Object entry : entries.values())
		{
			if (entry instanceof CompraRedis)
			{
				compras.add((CompraRedis) entry);
			}
		}
		return compras;
	}

	public void deleteById(String chave)
	{
		hashOperations.delete("CompraRedis", chave);
	}
}