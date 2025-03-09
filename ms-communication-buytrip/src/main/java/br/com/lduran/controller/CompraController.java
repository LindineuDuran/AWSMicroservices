package br.com.lduran.controller;

import br.com.lduran.model.CompraChaveDTO;
import br.com.lduran.model.CompraDTO;
import br.com.lduran.model.RetornoDTO;
import br.com.lduran.service.SnsNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@RestController
@RequestMapping("/")
public class CompraController
{
	@Autowired
	private SnsNotificationService notificationService;

	@RequestMapping(path = "/topicMessage", method = RequestMethod.POST)
	public ResponseEntity<RetornoDTO> pagamento(@Valid @NotNull @RequestBody CompraDTO compraDTO) throws Exception
	{
		CompraChaveDTO compraChaveDTO = new CompraChaveDTO();
		compraChaveDTO.setCompraDTO(compraDTO);
		compraChaveDTO.setChave(UUID.randomUUID().toString());

		ObjectMapper obj = new ObjectMapper();

		String jsonMessage = obj.writeValueAsString(compraChaveDTO);

		notificationService.sendNotification("Subject: Notification", jsonMessage, null);

		RetornoDTO retorno = new RetornoDTO();
		retorno.setMensagem("Compra registrada com sucesso. Aguarda a confirmação do pagamento.");
		retorno.setChavePesquisa(compraChaveDTO.getChave());

		return new ResponseEntity<RetornoDTO>(retorno, HttpStatus.OK);
	}
}