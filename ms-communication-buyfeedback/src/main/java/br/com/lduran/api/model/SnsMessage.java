package br.com.lduran.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SnsMessage
{
	@JsonProperty("Message")
	private String Message; // Corresponde à chave "Message" no JSON recebido
}