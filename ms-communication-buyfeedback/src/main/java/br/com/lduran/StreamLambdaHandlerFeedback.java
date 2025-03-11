package br.com.lduran;

import br.com.lduran.api.model.CompraFinalizadaDTO;
import br.com.lduran.api.model.MsgRecebidaDTO;
import br.com.lduran.domain.entity.CompraRedis;
import br.com.lduran.domain.service.RedisService;
import br.com.lduran.mapper.JsonProcessor;
import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//@Slf4j
public class StreamLambdaHandlerFeedback implements RequestStreamHandler
{
	private static final Logger log = LoggerFactory.getLogger(StreamLambdaHandlerFeedback.class);
	private RedisService redisService;

	@Value("${SNS_TOPIC_ARN_FINALIZADO}")
	private String snsTopicArnFinalizado;

	@Value("${sqs.delete.message:true}") // Define o comportamento padrão (apagar ou não)
	private boolean deleteMessage;

	private static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

	private static ApplicationContext context;
	private final JsonProcessor processor;
	private final SqsClient sqsClient;

	public StreamLambdaHandlerFeedback()
	{
		if (context == null) { context = new AnnotationConfigApplicationContext(App.class); }
		this.processor = context.getBean(JsonProcessor.class);
		this.sqsClient = context.getBean(SqsClient.class);
		this.redisService = context.getBean(RedisService.class); // Injeta o RedisService
	}

	static
	{
		try
		{
			// For applications that don't need async builder:
			//handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(App.class);

			// For applications that take longer than 10 seconds to start, use the async builder:
			handler = new SpringBootProxyHandlerBuilder<AwsProxyRequest>().defaultProxy().asyncInit().springBootApplication(App.class).buildAndInitialize();
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
		JsonNode jsonNode = objectMapper.readTree(inputStream);

		if (isSQSEvent(jsonNode))
		{
			ObjectNode objectNode = (ObjectNode) jsonNode;
			JsonNode recordsNode = objectNode.get("Records");
			if (recordsNode != null)
			{
				objectNode.set("records", recordsNode);
				objectNode.remove("Records"); // ✅ Agora funciona corretamente
			}

			SQSEvent sqsEvent = objectMapper.treeToValue(jsonNode, SQSEvent.class);
			handleSQSEvent(sqsEvent, context);
		}
		else
		{
			log.info("Detectado evento API Gateway");

			// 🔹 Conversão corrigida: Transformando byte[] de volta para InputStream
			byte[] jsonBytes = objectMapper.writeValueAsBytes(jsonNode);
			InputStream fixedInputStream = new ByteArrayInputStream(jsonBytes);

			handler.proxyStream(fixedInputStream, outputStream, context);
		}
	}

	private boolean isSQSEvent(JsonNode jsonNode)
	{
		return jsonNode.has("Records") && jsonNode.get("Records").isArray() && jsonNode.get("Records").get(0).has("eventSource") && "aws:sqs".equals(jsonNode.get("Records").get(0).get("eventSource").asText());
	}

	private void handleSQSEvent(SQSEvent event, Context context)
	{
		if (event.getRecords().isEmpty())
		{
			log.info("Nenhuma mensagem recebida.");
		}

		for (SQSEvent.SQSMessage message : event.getRecords())
		{
			processMessage(message);
		}
	}

	private void processMessage(SQSEvent.SQSMessage message)
	{
		try
		{
			String json = message.getBody();
			log.info("Processando mensagem: {}", json);

			if (snsTopicArnFinalizado == null)
			{
				snsTopicArnFinalizado= "arn:aws:sns:sa-east-1:000000000000:snsFilaComprasFinalizado";
			}

			MsgRecebidaDTO msgRecebidaDTO = processor.processJsonMessage(json);
			if (msgRecebidaDTO.getTopicArn().equals(snsTopicArnFinalizado))
			{
				log.info("Mensagem recebida:" + json);

				CompraFinalizadaDTO compraFinalizadaDTO = processor.processJsonCompraFinalizada(json);

				CompraRedis credis = new CompraRedis();
				credis.setId(compraFinalizadaDTO.getCompraChaveDTO().getChave());
				credis.setMensagem(compraFinalizadaDTO.getMensagem());
				credis.setNroCartao(compraFinalizadaDTO.getCompraChaveDTO().getCompraDTO().getNroCartao());
				credis.setValorPassagem(compraFinalizadaDTO.getCompraChaveDTO().getCompraDTO().getValorCompra());
				credis.setCodigoPassagem(compraFinalizadaDTO.getCompraChaveDTO().getCompraDTO().getCodigoPassagem());
				credis.setPagamentoOK(compraFinalizadaDTO.isPagamentoOK());

				log.info("Gravando no redis....{}", credis);
				redisService.salvar(credis.getId(), credis); // Usa o RedisService

				// Apaga a mensagem se configurado para tal
				if (deleteMessage)
				{
					deleteMessage(message.getReceiptHandle(), msgRecebidaDTO.getMessageId());
				}
			}
		}
		catch (Exception e)
		{
			log.error("Erro ao processar mensagem: {}", e.getMessage(), e);
		}
	}

	private void deleteMessage(String receiptHandle, String queueArn) {
		String queueUrl = queueArn.replace("arn:aws:sqs:sa-east-1:000000000000:", "http://localhost:4566/000000000000/");
		sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
	}
}
