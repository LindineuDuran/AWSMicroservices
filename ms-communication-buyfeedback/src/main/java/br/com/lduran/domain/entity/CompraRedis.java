package br.com.lduran.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import br.com.lduran.config.BigDecimalDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RedisHash("compra")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompraRedis implements Serializable
{
	@Id
	@JsonProperty("id")
	private String id;

	@JsonProperty("mensagem")
	private String mensagem;

	@JsonProperty("codigo_passagem")
	private Integer codigoPassagem;

	@JsonProperty("nro_cartao")
	private String nroCartao;

	@JsonProperty("valor_passagem")
	@JsonDeserialize(using = BigDecimalDeserializer.class)
	private BigDecimal valorPassagem;

	@JsonProperty("pagamento_ok")
	private boolean pagamentoOK;
}