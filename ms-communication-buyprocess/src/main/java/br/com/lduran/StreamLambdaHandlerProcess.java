package br.com.lduran;

import br.com.lduran.mapper.JsonProcessor;
import br.com.lduran.model.CompraChaveDTO;
import br.com.lduran.model.CompraFinalizadaDTO;
import br.com.lduran.model.MsgRecebidaDTO;
import br.com.lduran.service.BankService;
import br.com.lduran.service.SnsNotificationService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

@Slf4j
@Component
public class StreamLambdaHandlerProcess implements RequestHandler<SQSEvent, Void>
{
	@Value("${SNS_TOPIC_ARN_AGUARDANDO:arn:aws:sns:sa-east-1:000000000000:snsFilaComprasAguardando}")
	private String snsTopicArnAguardando;

	private static ApplicationContext context;
	private final JsonProcessor processor;
	private final BankService bank;
	private final SnsNotificationService notificationService;
	private final SqsClient sqsClient;

	// 🔹 Construtor sem argumentos para o AWS Lambda
	public StreamLambdaHandlerProcess()
	{
		if (context == null) {context = new AnnotationConfigApplicationContext(App.class);}

		this.processor = context.getBean(JsonProcessor.class);
		this.bank = context.getBean(BankService.class);
		this.notificationService = context.getBean(SnsNotificationService.class);
		this.sqsClient = context.getBean(SqsClient.class);
	}

	@Autowired
	public StreamLambdaHandlerProcess(JsonProcessor processor, BankService bank, SnsNotificationService notificationService, SqsClient sqsClient)
	{
		this.processor = processor;
		this.bank = bank;
		this.notificationService = notificationService;
		this.sqsClient = sqsClient;
	}

	@Override
	public Void handleRequest(SQSEvent event, Context context)
	{
		if (event.getRecords().isEmpty())
		{
			log.info("Nenhuma mensagem recebida.");
			return null;
		}

		for (SQSEvent.SQSMessage message : event.getRecords())
		{
			processMessage(message);
		}

		return null;
	}

	private void processMessage(SQSEvent.SQSMessage message)
	{
		try
		{
			String json = message.getBody();
			log.info("Processando mensagem: {}", json);

			if (snsTopicArnAguardando == null)
			{
				snsTopicArnAguardando = "arn:aws:sns:sa-east-1:000000000000:snsFilaComprasAguardando";
			}

			MsgRecebidaDTO msgRecebidaDTO = processor.processJsonMessage(json);
			if (snsTopicArnAguardando.equals(msgRecebidaDTO.getTopicArn()))
			{
				CompraChaveDTO compraChaveDTO = processor.processJsonCompraChave(json);
				//log.info("Compra Chave DTO: {}", compraChaveDTO);

				if (bank == null)
				{
					log.error("Serviço de Banco está nulo");
					return;
				}

				bank.pagar(compraChaveDTO).thenAccept(pg ->
				{
					log.info("PagamentoRetorno: {}", pg);

					CompraFinalizadaDTO compraFinalizadaDTO = new CompraFinalizadaDTO();
					compraFinalizadaDTO.setCompraChaveDTO(compraChaveDTO);
					compraFinalizadaDTO.setPagamentoOK(pg.isPagamentoOK());
					compraFinalizadaDTO.setMensagem(pg.getMensagem());

					String jsonFinalizado = null;
					try
					{
						jsonFinalizado = processor.geraJsonFinalizado(compraFinalizadaDTO);
					}
					catch (JsonProcessingException e)
					{
						throw new RuntimeException(e);
					}

					notificationService.sendNotification("Subject: Notification", jsonFinalizado, null);

					// Apagar a mensagem da fila após processamento
					deleteMessage(message.getReceiptHandle(), message.getEventSourceArn());
				});
			}
		}
		catch (Exception e)
		{
			log.error("Erro ao processar mensagem: ", e);
		}
	}

	private void deleteMessage(String receiptHandle, String queueArn)
	{
		String queueUrl = queueArn.replace("arn:aws:sqs:sa-east-1:000000000000:", "http://localhost:4566/000000000000/");
		sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
	}
}
