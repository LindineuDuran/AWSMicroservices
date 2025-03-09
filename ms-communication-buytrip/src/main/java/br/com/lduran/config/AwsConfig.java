package br.com.lduran.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig
{
	@Value("${AWS_ENDPOINT_URL}")
	private String awsEndpointUrl;

	@Value("${spring.cloud.aws.endpoint}")
	private String localstackEndpoint;

	@Value("${spring.cloud.aws.region.static}")
	private String region;

	@Value("${spring.cloud.aws.credentials.access-key}")
	private String accessKey;

	@Value("${spring.cloud.aws.credentials.secret-key}")
	private String secretKey;

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