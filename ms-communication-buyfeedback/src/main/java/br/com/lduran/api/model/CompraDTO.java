package br.com.lduran.api.model;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import br.com.lduran.config.BigDecimalDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraDTO
{
	@NotNull(message = "Código da passagem é obrigatório")
	@JsonProperty("codigo_passagem")
	private Integer codigoPassagem;

	@NotNull(message = "Número do cartão é obrigatório")
	@JsonProperty("nro_cartao")
	private String nroCartao;

	@NotNull(message = "Código de segurança do cartão é obrigatório")
	@JsonProperty("codigo_seguranca_cartao")
	private Integer codigoSegurancaCartao;

	@NotNull(message = "Valor da compra é obrigatório")
	@JsonProperty("valor_compra")
	@JsonDeserialize(using = BigDecimalDeserializer.class)
	private BigDecimal valorCompra;
}