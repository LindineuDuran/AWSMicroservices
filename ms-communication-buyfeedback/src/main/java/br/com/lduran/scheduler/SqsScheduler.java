/*
package br.com.lduran.scheduler;

import br.com.lduran.domain.service.SqsMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

@Slf4j
@Component
public class SqsScheduler
{
	@Value("${sqs.queue.name}")
	private String queueName;

	private final SqsMessageListener sqsMessageListener;
	private final SqsClient sqsClient;

	public SqsScheduler(SqsMessageListener sqsMessageListener, SqsClient sqsClient)
	{
		this.sqsMessageListener = sqsMessageListener;
		this.sqsClient = sqsClient;
	}

	@Scheduled(fixedDelay = 10000) // Executa a cada 10 segundos
	public void pollMessages() throws Exception
	{
		String queueUrl = getQueueUrl(queueName);
		if (queueUrl != null)
		{
			sqsMessageListener.listenToMessages(queueUrl);
		}
	}

	private String getQueueUrl(String queueName)
	{
		try
		{
			GetQueueUrlRequest request = GetQueueUrlRequest.builder()
														   .queueName(queueName)
														   .build();

			GetQueueUrlResponse response = sqsClient.getQueueUrl(request);
			return response.queueUrl();
		}
		catch (Exception e)
		{
			log.error("Erro ao buscar a URL da fila: " + e.getMessage());
			return null;
		}
	}
}
*/
