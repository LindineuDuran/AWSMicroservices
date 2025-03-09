@echo off
echo ### Criando Queue(Standard) no SQS do LocalStack...
aws --endpoint http://localhost:4566 --profile localstack sqs create-queue --queue-name sqsPassagens

REM Lista Filas Criadas
REM aws --endpoint http://localhost:4566 --profile localstack sqs list-queues

REM echo ### Envia Mensagem para a Fila
REM aws --endpoint http://localhost:4566 --profile localstack sqs send-message --queue-url http://localhost:4566/000000000000/sqsPassagens --message-body "Hello World SQS!!!" --delay-seconds 1

REM Verifica Sucesso no Envio da Mensagem
REM aws --endpoint http://localhost:4566 --profile localstack sqs receive-message --queue-url http://localhost:4566/000000000000/sqsPassagens

REM echo ### Envia Mensagem para a Fila
REM aws --endpoint-url=http://localhost:4566 --profile localstack sqs send-message --queue-url=http://localhost:4566/000000000000/sqsPassagens --message-body "{'id': '123', 'content': 'Test message'}"

REM Verifica Sucesso no Envio da Mensagem
REM aws --endpoint http://localhost:4566 --profile localstack sqs receive-message --queue-url http://localhost:4566/000000000000/sqsPassagens

echo ### Criando Queue(Standard) no SNS do LocalStack...
aws --endpoint http://localhost:4566 --profile localstack sns create-topic --name snsFilaComprasAguardando

echo ### Subscreve o topico snsFilaComprasAguardando a fila sqsPassagens do LocalStack...
aws --endpoint http://localhost:4566 --profile localstack sns subscribe --topic-arn arn:aws:sns:sa-east-1:000000000000:snsFilaComprasAguardando --protocol sqs --notification-endpoint arn:aws:sqs:sa-east-1:000000000000:sqsPassagens

echo ### Criando Queue(Standard) no SNS do LocalStack...
aws --endpoint http://localhost:4566 --profile localstack sns create-topic --name snsFilaComprasFinalizado

echo ### Subscreve o topico snsFilaComprasFinalizado a fila sqsPassagens do LocalStack...
aws --endpoint http://localhost:4566 --profile localstack sns subscribe --topic-arn arn:aws:sns:sa-east-1:000000000000:snsFilaComprasFinalizado --protocol sqs --notification-endpoint arn:aws:sqs:sa-east-1:000000000000:sqsPassagens