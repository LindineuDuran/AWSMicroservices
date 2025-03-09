package br.com.lduran.controller;

import br.com.lduran.entity.Cartao;
import br.com.lduran.model.RetornoDTO;
import br.com.lduran.service.CartaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cartao")
public class CartaoController
{
	@Autowired
	private CartaoService cartaoService;

	private final DynamoDbTable<Cartao> cartaoDynamoDbTable;

	public CartaoController(DynamoDbTable<Cartao> cartaoDynamoDbTable)
	{
		this.cartaoDynamoDbTable = cartaoDynamoDbTable;
	}

	@PostMapping
	public ResponseEntity<Cartao> createCartao(@RequestBody Cartao cartao)
	{
		// Salva cartão
		cartaoDynamoDbTable.putItem(cartao);

		// Busca um único cartao pelo `id`
		var key = Key.builder().partitionValue(cartao.getId()).build();
		var savedCartao = cartaoDynamoDbTable.getItem(r -> r.key(key));

		return savedCartao == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(savedCartao);
	}

	@PostMapping("/list")
	public ResponseEntity<Cartao> createCartaoList(@RequestBody List<Cartao> cartoes)
	{
		cartoes.forEach(cartaoDynamoDbTable::putItem);

		return ResponseEntity.status(204).build();
	}

	@GetMapping
	public ResponseEntity<List<Cartao>> listCartoes()
	{
		// Lista todos os itens da tabela
		var results = cartaoDynamoDbTable.scan().items().stream().collect(Collectors.toList());

		return ResponseEntity.ok(results);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Cartao> getCartaoById(@PathVariable("id") String id)
	{
		// Busca um único livro pelo `id`
		var key = Key.builder().partitionValue(id).build();
		var cartao = cartaoDynamoDbTable.getItem(r -> r.key(key));

		return cartao == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(cartao);
	}

	@GetMapping("/filtro")
	public ResponseEntity<?> getCartaoByNroAndCvv(@RequestParam("nroCartao") String nroCartao, @RequestParam("cvv") Integer cvv)
	{
		// Busca um único cartao pelo filtro
		Cartao cartao = cartaoService.getCartao(cvv, nroCartao).get();

		RetornoDTO retorno = new RetornoDTO();
		retorno.setMensagem(cartao.toString());

		return cartao == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(cartao);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Cartao> update(@PathVariable("id") String playerId, @RequestBody Cartao newCartao)
	{
		var key = Key.builder().partitionValue(playerId).build();
		var cartao = cartaoDynamoDbTable.getItem(r -> r.key(key));

		if (cartao == null)
		{
			return ResponseEntity.notFound().build();
		}

		cartao.setNroCartao(newCartao.getNroCartao());
		cartao.setCodigoSegurancaCartao(newCartao.getCodigoSegurancaCartao());
		cartao.setValorCredito(newCartao.getValorCredito());

		cartaoDynamoDbTable.putItem(cartao);

		var savedBook = cartaoDynamoDbTable.getItem(r -> r.key(key));

		return savedBook == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(savedBook);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") String playerId)
	{
		var key = Key.builder().partitionValue(playerId).build();
		var cartao = cartaoDynamoDbTable.getItem(r -> r.key(key));

		if (cartao == null)
		{
			return ResponseEntity.notFound().build();
		}

		cartaoDynamoDbTable.deleteItem(r -> r.key(key));
		return ResponseEntity.noContent().build();
	}
}