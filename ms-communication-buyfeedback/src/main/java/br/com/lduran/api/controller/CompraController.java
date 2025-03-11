package br.com.lduran.api.controller;

import br.com.lduran.domain.entity.CompraRedis;
import br.com.lduran.domain.service.RedisService;
import br.com.lduran.exceptions.NaoFinalizadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/feedback")
public class CompraController
{
	private static final Logger log = LoggerFactory.getLogger(CompraController.class);

	@Autowired
	private RedisService redisService;

	@RequestMapping(method = RequestMethod.GET)
	public List<CompraRedis> listar()
	{
		List<CompraRedis> compras = (List<CompraRedis>) redisService.findAll();

		return compras;
	}

	@RequestMapping(path = "/{chave}", method = RequestMethod.GET)
	public CompraRedis status(@PathVariable("chave") String chave)
	{
		Optional<CompraRedis> compra = redisService.buscar(chave);

		if (!compra.isPresent())
		{
			throw new NaoFinalizadoException();
		}

		return compra.get();
	}

	@RequestMapping(path = "/{chave}", method = RequestMethod.DELETE)
	public void excluir(@PathVariable("chave") String chave)
	{
		redisService.deleteById(chave);
	}

	@RequestMapping(path = "/meunome", method = RequestMethod.GET)
	public String status()
	{
		return "Estou na máquina do: LLDURAN";
	}
}