package br.com.lduran.mapper;

import br.com.lduran.api.model.CompraFinalizadaDTO;
import br.com.lduran.api.model.MsgRecebidaDTO;
import br.com.lduran.api.model.SnsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JsonProcessor
{
	public MsgRecebidaDTO processJsonMessage(String json) throws Exception
	{
		// Instancia o ObjectMapper
		ObjectMapper mapper = new ObjectMapper();

		// Passo 2: Deserializa o JSON do SNS para MsgRecebidaDTO
		MsgRecebidaDTO msgRecebidaDTO = mapper.readValue(json, MsgRecebidaDTO.class);

		// Retorna o objeto final deserializado
		return msgRecebidaDTO;
	}

	public CompraFinalizadaDTO processJsonCompraFinalizada(String json) throws Exception
	{
		// Instancia o ObjectMapper
		ObjectMapper mapper = new ObjectMapper();

		// Passo 2: Deserializa o JSON do SNS para a classe auxiliar
		SnsMessage snsMessage = mapper.readValue(json, SnsMessage.class);

		// Passo 3: Deserializa o conteúdo de "Message" para CompraChaveDTO
		CompraFinalizadaDTO compraFinalizadaDTO = mapper.readValue(snsMessage.getMessage(), CompraFinalizadaDTO.class);

		// Retorna o objeto final deserializado
		return compraFinalizadaDTO;
	}
}