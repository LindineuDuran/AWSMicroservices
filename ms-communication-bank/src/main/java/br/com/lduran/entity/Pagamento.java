package br.com.lduran.entity;

import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Setter
@DynamoDbBean
public class Pagamento
{
	private String id;
	private Cartao cartao;
	private BigDecimal valorCompra;

	public Pagamento()
	{
		this.id = Instant.now().toString() + "-" + UUID.randomUUID().toString();
	}

	@DynamoDbPartitionKey
	@DynamoDbAttribute("id")
	public String getId()
	{
		return id;
	}

	@DynamoDbAttribute("cartao")
	public Cartao getCartao()
	{
		return cartao;
	}

	@DynamoDbAttribute("valor_compra")
	public BigDecimal getValorCompra()
	{
		return valorCompra;
	}
}