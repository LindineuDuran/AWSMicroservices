package br.com.lduran.entity;

import br.com.lduran.config.BigDecimalDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@DynamoDbBean
public class Cartao
{
	private String id;
	private String nroCartao;
	private Integer codigoSegurancaCartao;
	private BigDecimal valorCredito;

	public Cartao()
	{
		this.id = UUID.randomUUID().toString();
	}

	@DynamoDbPartitionKey
	@DynamoDbAttribute("id")
	public String getId()
	{
		return id;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = "CartaoIndex")
	@DynamoDbAttribute("nro_cartao")
	@JsonProperty("nro_cartao")
	public String getNroCartao() { return nroCartao; }

	@DynamoDbSecondarySortKey(indexNames = "CartaoIndex")
	@DynamoDbAttribute("codigo_seguranca_cartao")
	@JsonProperty("codigo_seguranca_cartao")
	public Integer getCodigoSegurancaCartao() { return codigoSegurancaCartao; }

	@DynamoDbAttribute("valor_credito")
	@JsonProperty("valor_credito")
	@JsonDeserialize(using = BigDecimalDeserializer.class)
	public BigDecimal getValorCredito() { return valorCredito; }
}