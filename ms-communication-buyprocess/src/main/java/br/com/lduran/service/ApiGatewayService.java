package br.com.lduran.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.apigateway.ApiGatewayAsyncClient;
import software.amazon.awssdk.services.apigateway.model.GetRestApisRequest;
import software.amazon.awssdk.services.apigateway.model.GetRestApisResponse;

import java.util.concurrent.CompletableFuture;

@Service
public class ApiGatewayService
{
	private final ApiGatewayAsyncClient apiGatewayClient;

	public ApiGatewayService(ApiGatewayAsyncClient apiGatewayClient)
	{
		this.apiGatewayClient = apiGatewayClient;
	}

	public CompletableFuture<GetRestApisResponse> listApis()
	{
		// Criando uma requisição para listar as APIs REST
		GetRestApisRequest request = GetRestApisRequest.builder().build();

		// Usando o cliente para fazer a chamada correta
		return apiGatewayClient.getRestApis(request).thenApply(result ->
		{
			// Imprimindo informações sobre as APIs
			result.items().forEach(api ->
			{
				System.out.println("API Name: " + api.name());
				System.out.println("API ID: " + api.id());
			});
			return result;
		});
	}
}
