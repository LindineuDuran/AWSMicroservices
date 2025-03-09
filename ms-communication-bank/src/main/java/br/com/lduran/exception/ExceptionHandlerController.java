package br.com.lduran.exception;

import br.com.lduran.model.RetornoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionHandlerController
{
	@ExceptionHandler(CartaoException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	@ResponseBody
	public RetornoDTO handleCartaoException(CartaoException ex) { return new RetornoDTO(ex.getMessage()); }

	@ExceptionHandler(PagamentoException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ResponseBody
	public RetornoDTO process(RuntimeException ex)
	{
		return new RetornoDTO(ex.getMessage());
	}
}