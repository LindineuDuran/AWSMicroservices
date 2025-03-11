package br.com.lduran.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class SqsConfig
{
	@Value("${AWS_ENDPOINT_URL:http://host.docker.internal:4566}")
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
	public SqsClient sqsClient()
	{
		String endpoint = awsEndpointUrl;
		if (endpoint == null) {
			endpoint = localstackEndpoint; // Valor padrão para LocalStack
		}

		return SqsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey))) // Credenciais para o LocalStack
				.region(Region.of(region)) // Certifique-se de usar a mesma região configurada
				.endpointOverride(URI.create(endpoint)) // Aponta para o LocalStack
				.build();
	}
}