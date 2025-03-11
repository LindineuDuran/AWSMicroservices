package br.com.lduran.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Configuration
public class AwsConfig
{
	@Value("${AWS_ENDPOINT_URL}")
	private String awsEndpointUrl;

	@Value("${SPRING_CLOUD_AWS_ENDPOINT:http://localhost:4566}")
	private String localstackEndpoint;

	@Value("${SPRING_CLOUD_AWS_REGION_STATIC:sa-east-1}")
	private String region;

	@Value("${SPRING_CLOUD_AWS_CREDENTIALS_ACCESS_KEY:test}")
	private String accessKey;

	@Value("${SPRING_CLOUD_AWS_CREDENTIALS_SECRET_KEY:test}")
	private String secretKey;

	@Bean
	public ApiGatewayAsyncClient amazonApiGateway()
	{
		String endpointUrl = defineEndpointUrl();

		// Criando o HttpClient com NettyHttpClient
		return ApiGatewayAsyncClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
				.region(Region.of(region))
				.endpointOverride(URI.create(endpointUrl))  // Definir o endpoint para o LocalStack
				.httpClient(NettyNioAsyncHttpClient.create())  // Configuração do cliente assíncrono
				.overrideConfiguration(ClientOverrideConfiguration.builder()
						.apiCallTimeout(Duration.ofSeconds(10))
						.apiCallAttemptTimeout(Duration.ofSeconds(5)).build()).build();
	}

	@Bean
	public SnsClient snsClient()
	{
		String endpointUrl = defineEndpointUrl();

		return SnsClient.builder()
				.region(Region.of(region)) // Certifique-se de usar a mesma região configurada
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey))) // Credenciais para o LocalStack
				.endpointOverride(URI.create(endpointUrl)) // Aponta para o LocalStack
				.build();
	}

	@Bean
	public SqsClient sqsClient()
	{
		String endpointUrl = defineEndpointUrl();

		return SqsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey))) // Credenciais para o LocalStack
				.region(Region.of(region)) // Certifique-se de usar a mesma região configurada
				.endpointOverride(URI.create(endpointUrl)) // Aponta para o LocalStack
				.build();
	}

	private String defineEndpointUrl()
	{
		String endpointUrl = awsEndpointUrl;
		if (endpointUrl == null)
		{
			endpointUrl = localstackEndpoint; // Valor padrão para LocalStack
		}

		return endpointUrl;
	}
}
