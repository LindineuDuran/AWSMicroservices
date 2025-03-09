package br.com.lduran.service;

import br.com.lduran.entity.Cartao;
import br.com.lduran.entity.Pagamento;
import br.com.lduran.exception.PagamentoException;
import br.com.lduran.model.PagamentoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import javax.transaction.Transactional;

@Service
public class PagamentoService
{
	@Autowired
	private CartaoService cartaoService;

	private final DynamoDbTable<Pagamento> pagamentoDynamoDbTable;

	public PagamentoService(DynamoDbTable<Pagamento> pagamentoDynamoDbTable)
	{
		this.pagamentoDynamoDbTable = pagamentoDynamoDbTable;
	}

	@Transactional
	public void pagamento(PagamentoDTO pagamentoDTO)
	{
		if (!cartaoService.isValido(pagamentoDTO.getCodigoSegurancaCartao(), pagamentoDTO.getNroCartao()))
		{
			throw new PagamentoException("Cartão inválido.");
		}

		if(pagamentoDTO.getValorCompra() == null)
		{
			throw new PagamentoException("Valor da compra não pode ser nulo!");
		}

		if (!cartaoService.isSaldoSuficiente(pagamentoDTO.getCodigoSegurancaCartao(),
				pagamentoDTO.getNroCartao(),
				pagamentoDTO.getValorCompra()))
		{
			throw new PagamentoException("Cartão não possui saldo suficiente.");
		}

		Pagamento pagamento = new Pagamento();
		pagamento.setValorCompra(pagamentoDTO.getValorCompra());

		Cartao cartao = cartaoService.getCartao(pagamentoDTO.getCodigoSegurancaCartao(), pagamentoDTO.getNroCartao()).get();

		cartao.setValorCredito(cartao.getValorCredito().subtract(pagamentoDTO.getValorCompra()));

		pagamento.setCartao(cartao);

		pagamentoDynamoDbTable.putItem(pagamento);

		cartaoService.atualizarSaldo(pagamentoDTO.getCodigoSegurancaCartao(), pagamentoDTO.getNroCartao(),
				pagamentoDTO.getValorCompra());
	}
}