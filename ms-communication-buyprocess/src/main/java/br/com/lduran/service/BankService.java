package br.com.lduran.service;

import br.com.lduran.model.BankRetornoDTO;
import br.com.lduran.model.CompraChaveDTO;
import br.com.lduran.model.PagamentoDTO;
import br.com.lduran.model.PagamentoRetorno;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.apigateway.model.GetRestApisResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class BankService
{
	@Value("${AWS_ENDPOINT_URL:http://host.docker.internal:4566}")
	private String awsEndpointUrl;

	@Value("${SPRING_CLOUD_AWS_ENDPOINT:http://localhost:4566}")
	private String localstackEndpoint;

	private final ApiGatewayService apiGatewayService;
	private final RestTemplate restTemplate;

	public BankService(ApiGatewayService apiGatewayService)
	{
		this.apiGatewayService = apiGatewayService;
		this.restTemplate = new RestTemplate();
	}

	public CompletableFuture<PagamentoRetorno> pagar(CompraChaveDTO compraChaveDTO)
	{
		log.info("Compra Chave DTO - pagar: {}", compraChaveDTO);

		PagamentoDTO json = new PagamentoDTO();
		json.setNroCartao(compraChaveDTO.getCompraDTO().getNroCartao());
		json.setCodigoSegurancaCartao(compraChaveDTO.getCompraDTO().getCodigoSegurancaCartao());
		json.setValorCompra(compraChaveDTO.getCompraDTO().getValorCompra());

		return apiGatewayService.listApis()
				                .thenApply(result -> buscarApiId(result))
				                .thenApply(apiId -> construirUrl(apiId))
				                .thenApply(link -> enviarPagamento(link, json))
				                .exceptionally(ex ->
								{
									log.error("Erro no pagamento: {}", ex.getMessage());
									return new PagamentoRetorno("Erro ao processar pagamento", false);
								});
	}

	private String buscarApiId(GetRestApisResponse result)
	{
		return result.items()
				     .stream()
				     .filter(api -> "StreamLambdaHandlerBank".equals(api.name()))
				     .map(api -> api.id())
				     .findFirst().orElseThrow(() -> new RuntimeException("API Gateway 'StreamLambdaHandlerBank' not found"));
	}

	private String construirUrl(String apiId)
	{
		String endpointUrl = awsEndpointUrl;
		if (endpointUrl == null)
		{
			endpointUrl = localstackEndpoint; // Valor padrão para LocalStack
		}

		return String.format("%s/_aws/execute-api/%s/prod/pagamento/pagar", endpointUrl, apiId);
	}

	private PagamentoRetorno enviarPagamento(String link, PagamentoDTO json)
	{
		try
		{
			//log.info("bank.link: {}", link);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<PagamentoDTO> entity = new HttpEntity<>(json, headers);

			ResponseEntity<BankRetornoDTO> bankRetorno = restTemplate.exchange(link, HttpMethod.POST, entity, BankRetornoDTO.class);

			log.info("PagamentoRetorno: {}", bankRetorno);

			return new PagamentoRetorno(bankRetorno.getBody().getMensagem(), true);
		}
		catch (HttpClientErrorException ex)
		{
			log.error("Erro no método pagar: {}", ex.getMessage());
			return tratarErroPagamento(ex);
		}
	}

	private PagamentoRetorno tratarErroPagamento(HttpClientErrorException ex)
	{
		try
		{
			if (ex.getStatusCode() == HttpStatus.BAD_REQUEST)
			{
				ObjectMapper mapper = new ObjectMapper();
				BankRetornoDTO obj = mapper.readValue(ex.getResponseBodyAsString(), BankRetornoDTO.class);
				return new PagamentoRetorno(obj.getMensagem(), false);
			}
		}
		catch (IOException e)
		{
			log.error("Erro ao processar resposta JSON do erro: {}", e.getMessage());
		}
		return new PagamentoRetorno("Erro desconhecido", false);
	}
}
