package br.com.lduran.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MsgRecebidaDTO
{
	@JsonProperty("Type")
	private String type;

	@JsonProperty("MessageId")
	private String messageId;

	@JsonProperty("TopicArn")
	private String topicArn;

	@JsonProperty("Message")
	private String message;

	@JsonProperty("Timestamp")
	private String timestamp;
}