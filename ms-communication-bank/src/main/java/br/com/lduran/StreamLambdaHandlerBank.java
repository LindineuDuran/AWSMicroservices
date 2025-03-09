package br.com.lduran;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public class StreamLambdaHandlerBank implements RequestStreamHandler
{
	private static final String DYNAMODB_ENDPOINT = "http://host.docker.internal:4566"; // Usando host.docker.internal para LocalStack

	private static final DynamoDbClient dynamoDbClient;

	private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

	static
	{
		DynamoDbClientBuilder clientBuilder = DynamoDbClient.builder();
		clientBuilder.endpointOverride(URI.create(DYNAMODB_ENDPOINT));
		dynamoDbClient = clientBuilder.build();

		try
		{
			handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(App.class);
			// For applications that take longer than 10 seconds to start, use the async builder:
			// handler = new SpringBootProxyHandlerBuilder<AwsProxyRequest>()
			//                    .defaultProxy()
			//                    .asyncInit()
			//                    .springBootApplication(Application.class)
			//                    .buildAndInitialize();
		}
		catch (ContainerInitializationException e)
		{
			// if we fail here. We re-throw the exception to force another cold start
			e.printStackTrace();
			throw new RuntimeException("Could not initialize Spring Boot application", e);
		}
	}

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
	{
		handler.proxyStream(inputStream, outputStream, context);
	}
}