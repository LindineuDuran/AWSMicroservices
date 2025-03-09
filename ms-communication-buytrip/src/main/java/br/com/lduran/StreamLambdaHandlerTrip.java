package br.com.lduran;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class StreamLambdaHandlerTrip implements RequestStreamHandler
{
	private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

	static
	{
		try
		{
			// For applications that don't need async builder:
			handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(App.class);

			// For applications that take longer than 10 seconds to start, use the async builder:
			/*handler = new SpringBootProxyHandlerBuilder<AwsProxyRequest>()
					.defaultProxy()
					.asyncInit()
					.springBootApplication(App.class)
					.buildAndInitialize();*/
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
		// Lê o conteúdo do InputStream antes de passar para o handler
		StringBuilder event = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		while ((line = reader.readLine()) != null) {
			event.append(line);
		}

		// Loga a requisição recebida
		log.info("Received event: {}", event.toString());

		// Agora você pode processar o evento manualmente ou passar para o handler
		// Se você precisar, pode usar o conteúdo do 'event' para criar um novo InputStream
		InputStream processedInputStream = new ByteArrayInputStream(event.toString().getBytes(StandardCharsets.UTF_8));

		// Passa o InputStream para o handler
		handler.proxyStream(processedInputStream, outputStream, context);
	}
}