package br.com.lduran.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.UUID;

@Slf4j
@Service
public class SnsNotificationService
{
	@Value("${SNS_TOPIC_ARN_FINALIZADO:arn:aws:sns:sa-east-1:000000000000:snsFilaComprasFinalizado}")
	private String snsTopicArnFinalizado;

	private final SnsClient snsClient;

	public SnsNotificationService(SnsClient snsClient) { this.snsClient = snsClient; }

	public void sendNotification(String subject, String message, String topicArnOverride)
	{
		String targetTopicArn = (topicArnOverride != null) ? topicArnOverride : snsTopicArnFinalizado;

		String uniqueMsgId = UUID.randomUUID().toString();

		PublishRequest request = PublishRequest.builder()
				.topicArn(targetTopicArn)
				.subject(subject)
				.message(message)
				.build();

		PublishResponse response = snsClient.publish(request);
		log.info("Message sent to SNS with ID: {}", response.messageId());
	}
}
