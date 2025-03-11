package br.com.lduran.mapper;

import br.com.lduran.model.CompraChaveDTO;
import br.com.lduran.model.CompraFinalizadaDTO;
import br.com.lduran.model.MsgRecebidaDTO;
import br.com.lduran.model.SnsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JsonProcessor
{
	private final ObjectMapper mapper;

	public JsonProcessor() { this.mapper = new ObjectMapper(); }

	/*
	  Deserializa o JSON do SNS para MsgRecebidaDTO
	  Retorna o objeto final deserializado
	 */
	public MsgRecebidaDTO processJsonMessage(String json) throws Exception
	{
		return mapper.readValue(json, MsgRecebidaDTO.class);
	}

	public CompraChaveDTO processJsonCompraChave(String json) throws Exception
	{
		// Deserializa o JSON do SNS para a classe auxiliar
		SnsMessage snsMessage = mapper.readValue(json, SnsMessage.class);

		if (snsMessage == null || snsMessage.getMessage() == null) {
			throw new IllegalArgumentException("Erro: Mensagem SNS não contém campo 'Message'");
		}

		// Retorna o objeto final deserializado
		return mapper.readValue(snsMessage.getMessage(), CompraChaveDTO.class);
	}

	public String geraJsonFinalizado(CompraFinalizadaDTO compraFinalizadaDTO) throws JsonProcessingException
	{
		return mapper.writeValueAsString(compraFinalizadaDTO);
	}
}
