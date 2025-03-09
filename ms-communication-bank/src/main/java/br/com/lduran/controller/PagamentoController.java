package br.com.lduran.controller;

import br.com.lduran.model.PagamentoDTO;
import br.com.lduran.model.RetornoDTO;
import br.com.lduran.service.PagamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamento")
public class PagamentoController
{
	@Autowired
	private PagamentoService pagamentoService;

	@RequestMapping(path = "/pagar", method = RequestMethod.POST)
	public ResponseEntity<RetornoDTO> pagamento(@RequestBody PagamentoDTO pagamentoDTO)
	{
		pagamentoService.pagamento(pagamentoDTO);

		RetornoDTO retorno = new RetornoDTO();
		retorno.setMensagem("Pagamento registrado com sucesso");

		return new ResponseEntity<RetornoDTO>(retorno, HttpStatus.OK);
	}
}