package br.com.lduran.config;

import br.com.lduran.entity.Cartao;
import br.com.lduran.entity.Pagamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class DynamoDbConfig
{
	@Value("${spring.cloud.aws.endpoint.dynamodb}")
	private String dynamoDbEndpoint;

	@Value("${spring.cloud.aws.region.static}")
	private String region;

	@Value("${spring.cloud.aws.credentials.access-key}")
	private String accessKey;

	@Value("${spring.cloud.aws.credentials.secret-key}")
	private String secretKey;

	@Value("${app.aws.dynamodb.tables.cartao}")
	private String cartaoTableName;

	@Value("${app.aws.dynamodb.tables.pagamento}")
	private String pagamentoTableName;

	@EventListener(ApplicationReadyEvent.class)
	public void init()
	{
		System.out.println("DynamoDB Endpoint configurado como: " + dynamoDbEndpoint);
	}

	@Bean
	public DynamoDbClient dynamoDbClient()
	{
		String endpointDB = System.getenv("AWS_ENDPOINT_URL");
		if (endpointDB == null) {
			endpointDB = dynamoDbEndpoint; // Valor padrão para LocalStack
		}

		return DynamoDbClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
				.region(Region.of(region))
				.endpointOverride(URI.create(endpointDB)) // LocalStack endpoint
				.build();
	}

	@Bean
	public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient)
	{
		return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
	}

	@Bean
	public DynamoDbTable<Cartao> cartaoDynamoDbTable(DynamoDbEnhancedClient dynamoDbEnhancedClient)
	{
		// Criando a tabela para acessar a tabela e os índices
		return dynamoDbEnhancedClient.table(cartaoTableName, TableSchema.fromBean(Cartao.class));
	}

	@Bean
	public DynamoDbTable<Pagamento> pagamentoDynamoDbTable(DynamoDbEnhancedClient dynamoDbEnhancedClient)
	{
		return dynamoDbEnhancedClient.table(pagamentoTableName, TableSchema.fromBean(Pagamento.class));
	}
}