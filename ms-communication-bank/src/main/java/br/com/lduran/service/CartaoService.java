package br.com.lduran.service;

import br.com.lduran.entity.Cartao;
import br.com.lduran.exception.CartaoException;
import br.com.lduran.exception.PagamentoException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartaoService
{
	private final DynamoDbTable<Cartao> cartaoDynamoDbTable;

	public CartaoService(DynamoDbTable<Cartao> cartaoDynamoDbTable)
	{
		this.cartaoDynamoDbTable = cartaoDynamoDbTable;
	}

	public Optional<Cartao> getCartao(Integer codigoSegurancaCartao, String nroCartao)
	{
		// Cria a consulta no índice CartaoIndex
		QueryEnhancedRequest queryRequest = getQueryCartoes(codigoSegurancaCartao, nroCartao);

		// Consulta o índice CartaoIndex
		Optional<Cartao> cartao = cartaoDynamoDbTable.index("CartaoIndex") // Acessa o índice secundário
				.query(queryRequest) // Executa a query
				.stream() // Transforma o resultado em Stream
				.flatMap(page -> page.items().stream()) // Extrai os itens das páginas
				.findFirst(); // Retorna o primeiro item, se existir

		if (cartao.isEmpty())
		{
			throw new CartaoException("Cartão não cadastrado!");
		}

		return cartao;
	}

	public boolean isValido(Integer codigoSegurancaCartao, String nroCartao)
	{
		// Executa o scan e verifica se há resultados
		return getCartao(codigoSegurancaCartao, nroCartao).isPresent();
	}

	public boolean isSaldoSuficiente(Integer codigoSegurancaCartao, String nroCartao, BigDecimal valorCompra)
	{
		Optional<Cartao> cartao = getCartao(codigoSegurancaCartao,nroCartao);
		if(cartao.isEmpty())
		{
			throw new PagamentoException("Cartão não cadastrado!");
		}

		return cartao.get().getValorCredito().compareTo(valorCompra) >= 0;
	}

	@Transactional
	public void atualizarSaldo(Integer codigoSegurancaCartao, String nroCartao, BigDecimal valorCompra)
	{
		Optional<Cartao> cartao = getCartao(codigoSegurancaCartao,nroCartao);
		if(cartao.isEmpty())
		{
			throw new PagamentoException("Cartão não cadastrado!");
		}

		Cartao cartaoUpdated = cartao.get();
		cartaoUpdated.setValorCredito(cartaoUpdated.getValorCredito().subtract(valorCompra));
		cartaoDynamoDbTable.updateItem(cartaoUpdated);
	}

	private static QueryEnhancedRequest getQueryCartoes(Integer codigoSegurancaCartao, String nroCartao)
	{
		// Condição de consulta para o índice secundário
		QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue(nroCartao).sortValue(codigoSegurancaCartao));

		// Cria a consulta no índice CartaoIndex
		QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder().queryConditional(queryConditional).build();

		return queryRequest;
	}
}