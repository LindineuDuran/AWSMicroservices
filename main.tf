terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region                      = "sa-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true
  endpoints {
    iam         = "http://localhost:4566"
    lambda      = "http://localhost:4566"
    apigateway  = "http://localhost:4566"
    dynamodb    = "http://localhost:4566"
    sqs         = "http://localhost:4566"
    sns         = "http://localhost:4566"
  }
}

# Criando a fila SQS
resource "aws_sqs_queue" "sqs_passagens" {
  name = "sqs_passagens"
}

# Criando os tópicos SNS
resource "aws_sns_topic" "sns_fila_compras_aguardando" {
  name = "snsFilaComprasAguardando"
}

# Assinando a fila SQS aos tópicos SNS
resource "aws_sns_topic_subscription" "sns_fila_compras_aguardando_sub" {
  topic_arn = aws_sns_topic.sns_fila_compras_aguardando.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.sqs_passagens.arn
}

resource "aws_sns_topic" "sns_fila_compras_finalizado" {
  name = "snsFilaComprasFinalizado"
}

resource "aws_sns_topic_subscription" "sns_fila_compras_finalizado_sub" {
  topic_arn = aws_sns_topic.sns_fila_compras_finalizado.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.sqs_passagens.arn
}

# Criando permissão para SNS publicar mensagens na fila SQS
resource "aws_sqs_queue_policy" "sqs_passagens_policy" {
  queue_url = aws_sqs_queue.sqs_passagens.id
  policy    = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = "*"
        Action = "sqs:SendMessage"
        Resource = aws_sqs_queue.sqs_passagens.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = [
              aws_sns_topic.sns_fila_compras_aguardando.arn,
              aws_sns_topic.sns_fila_compras_finalizado.arn
            ]
          }
        }
      }
    ]
  })
}

resource "aws_iam_role" "lambda_execution_role" {
  name = "lambda-execution"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_policy" "lambda_policy" {
  name        = "lambda_policy"
  description = "Permissões para o Lambda acessar SNS e SQS"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "sqs:ListQueues",
          "sqs:GetQueueUrl",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = "arn:aws:sqs:sa-east-1:000000000000:sqs_passagens"
      },
      {
        Effect = "Allow"
        Action = "sns:Publish"
        Resource = "arn:aws:sns:sa-east-1:000000000000:snsFilaComprasAguardando"
      },
      {
        Effect = "Allow"
        Action = "sns:Publish"
        Resource = "arn:aws:sns:sa-east-1:000000000000:snsFilaComprasFinalizado"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic_policy_attachment" {
  role       = aws_iam_role.lambda_execution_role.name
  policy_arn = aws_iam_policy.lambda_policy.arn
}

# DynamoDB Table "Cartao"
resource "aws_dynamodb_table" "cartao" {
  name           = "Cartao"
  billing_mode   = "PROVISIONED"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "nro_cartao"
    type = "S"
  }

  attribute {
    name = "codigo_seguranca_cartao"
    type = "N"
  }

  global_secondary_index {
    name               = "CartaoIndex"
    hash_key           = "nro_cartao"
    range_key          = "codigo_seguranca_cartao"
    projection_type    = "ALL"
    read_capacity      = 5
    write_capacity     = 5
  }
}

# DynamoDB Table "Pagamento"
resource "aws_dynamodb_table" "pagamento" {
  name           = "Pagamento"
  billing_mode   = "PROVISIONED"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }
}

# Criação do Lambda para Bank
resource "aws_lambda_function" "ms-communication-bank_lambda" {
  function_name    = "StreamLambdaHandlerBank"
  handler          = "br.com.lduran.StreamLambdaHandlerBank::handleRequest"
  runtime          = "java11"
  memory_size      = 512
  timeout          = 30
  filename         = "ms-communication-bank/target/ms-communication-bank-0.0.1-SNAPSHOT-lambda-package.zip" # Caminho para o JAR
  source_code_hash = filebase64sha256("ms-communication-bank/target/ms-communication-bank-0.0.1-SNAPSHOT-lambda-package.zip")
  role             = aws_iam_role.lambda_execution_role.arn

  environment {
    variables = {
      TABLE_CARTAO: aws_dynamodb_table.cartao.name
      TABLE_PAGAMENTO: aws_dynamodb_table.pagamento.name
      AWS_ENDPOINT_URL  = "http://host.docker.internal:4566" # Aponta para o LocalStack
    }
  }
}

# Configuração da API para Bank
resource "aws_api_gateway_rest_api" "ms-communication-bank_lambda_api" {
  name = "StreamLambdaHandlerBank"
  description = "API Microsservice Bank"
}

# Root resource for /cartao
resource "aws_api_gateway_resource" "cartao" {
  rest_api_id = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  parent_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.root_resource_id
  path_part   = "cartao"
}

# GET /cartao - List all cartoes
resource "aws_api_gateway_method" "list_cartoes" {
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id   = aws_api_gateway_resource.cartao.id
  http_method   = "GET"
  authorization = "NONE"
}

# Definindo a integração com o Lambda
resource "aws_api_gateway_integration" "list_cartoes_integration" {
  rest_api_id             = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id             = aws_api_gateway_resource.cartao.id
  http_method             = aws_api_gateway_method.list_cartoes.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.ms-communication-bank_lambda.invoke_arn
}

# Resource for /cartao/{id}
resource "aws_api_gateway_resource" "cartao_id" {
  rest_api_id = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  parent_id   = aws_api_gateway_resource.cartao.id
  path_part   = "{id}"
}

# GET /cartao/{id} - Get a cartao by ID
resource "aws_api_gateway_method" "get_cartao_by_id" {
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id   = aws_api_gateway_resource.cartao_id.id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "get_cartao_by_id_integration" {
  rest_api_id             = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id             = aws_api_gateway_resource.cartao_id.id
  http_method             = aws_api_gateway_method.get_cartao_by_id.http_method
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.ms-communication-bank_lambda.invoke_arn
}

# POST /cartao - Create a new cartao
resource "aws_api_gateway_method" "create_cartao" {
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id   = aws_api_gateway_resource.cartao.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "post_cartoes_integration" {
  rest_api_id             = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id             = aws_api_gateway_resource.cartao.id
  http_method             = aws_api_gateway_method.create_cartao.http_method
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.ms-communication-bank_lambda.invoke_arn
}

# Resource for /cartao/list
resource "aws_api_gateway_resource" "cartao_list" {
  rest_api_id = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  parent_id   = aws_api_gateway_resource.cartao.id
  path_part   = "list"
}

# POST /cartao/list - Create a cartoes list
resource "aws_api_gateway_method" "create_cartao_list" {
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id   = aws_api_gateway_resource.cartao_list.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "post_cartoes_list_integration" {
  rest_api_id             = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id             = aws_api_gateway_resource.cartao_list.id
  http_method             = aws_api_gateway_method.create_cartao_list.http_method
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.ms-communication-bank_lambda.invoke_arn
}

# PUT /cartao/{id} - Update a cartao
resource "aws_api_gateway_method" "update_cartao" {
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id   = aws_api_gateway_resource.cartao_id.id
  http_method   = "PUT"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "put_cartoes_integration" {
  rest_api_id             = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id             = aws_api_gateway_resource.cartao_id.id
  http_method             = aws_api_gateway_method.update_cartao.http_method
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.ms-communication-bank_lambda.invoke_arn
}

# DELETE /cartao/{id} - Delete a cartao
resource "aws_api_gateway_method" "delete_cartao" {
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id   = aws_api_gateway_resource.cartao_id.id
  http_method   = "DELETE"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "delete_cartoes_integration" {
  rest_api_id             = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id             = aws_api_gateway_resource.cartao_id.id
  http_method             = aws_api_gateway_method.delete_cartao.http_method
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.ms-communication-bank_lambda.invoke_arn
}

# Root resource for /pagamento
resource "aws_api_gateway_resource" "pagamento" {
  rest_api_id = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  parent_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.root_resource_id
  path_part   = "pagamento"
}

# Root resource for /pagamento/pagar
resource "aws_api_gateway_resource" "pagamento_pagar" {
  rest_api_id = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  parent_id   = aws_api_gateway_resource.pagamento.id
  path_part   = "pagar"
}

# POST /pagamento/pagar - Create a new pagamento
resource "aws_api_gateway_method" "pagamento" {
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id   = aws_api_gateway_resource.pagamento_pagar.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "post_pagamentos_integration" {
  rest_api_id             = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  resource_id             = aws_api_gateway_resource.pagamento_pagar.id
  http_method             = aws_api_gateway_method.pagamento.http_method
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.ms-communication-bank_lambda.invoke_arn
}

# Criando o deployment
resource "aws_api_gateway_deployment" "ms-communication-bank_lambda_api_deployment" {
  depends_on  = [
    aws_api_gateway_integration.list_cartoes_integration,
    aws_api_gateway_integration.get_cartao_by_id_integration,
    aws_api_gateway_integration.post_cartoes_integration,
    aws_api_gateway_integration.post_cartoes_list_integration,
    aws_api_gateway_integration.put_cartoes_integration,
    aws_api_gateway_integration.delete_cartoes_integration,
    aws_api_gateway_integration.post_pagamentos_integration
  ]
  rest_api_id = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
}

# Definindo o estágio "prod" de forma explícita
resource "aws_api_gateway_stage" "prod_stage_bank" {
  deployment_id = aws_api_gateway_deployment.ms-communication-bank_lambda_api_deployment.id
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-bank_lambda_api.id
  stage_name    = "prod"
}

# Permissão para o API Gateway invocar o Lambda
resource "aws_lambda_permission" "apigw_invoke_bank" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.ms-communication-bank_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.ms-communication-bank_lambda_api.execution_arn}/*/*"
}

# Criação do Lambda para Buytrip
resource "aws_lambda_function" "ms-communication-buytrip_lambda" {
  function_name    = "StreamLambdaHandlerTrip"
  handler          = "br.com.lduran.StreamLambdaHandlerTrip::handleRequest"
  runtime          = "java11"
  memory_size      = 512
  timeout          = 30
  filename         = "ms-communication-buytrip/target/ms-communication-buytrip-0.0.1-SNAPSHOT-lambda-package.zip"
  source_code_hash = filebase64sha256("ms-communication-buytrip/target/ms-communication-buytrip-0.0.1-SNAPSHOT-lambda-package.zip")
  role             = aws_iam_role.lambda_execution_role.arn

  environment {
    variables = {
      AWS_ENDPOINT_URL = "http://host.docker.internal:4566"
    }
  }
}

# Configuração da API para Buytrip
resource "aws_api_gateway_rest_api" "ms-communication-buytrip_lambda_api" {
  name        = "StreamLambdaHandlerTrip"
  description = "API Microsservice Buytrip"
}

# Root resource for /topicMessage
resource "aws_api_gateway_resource" "topicMessage" {
  rest_api_id = aws_api_gateway_rest_api.ms-communication-buytrip_lambda_api.id
  parent_id   = aws_api_gateway_rest_api.ms-communication-buytrip_lambda_api.root_resource_id
  path_part   = "topicMessage"
}

# POST /topicMessage - Create a new pagamento
resource "aws_api_gateway_method" "create_pagamento" {
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-buytrip_lambda_api.id
  resource_id   = aws_api_gateway_resource.topicMessage.id  # Corrigido: 'topic_message' para 'topicMessage'
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "post_pagamentos_trip_integration" {
  rest_api_id             = aws_api_gateway_rest_api.ms-communication-buytrip_lambda_api.id
  resource_id             = aws_api_gateway_resource.topicMessage.id  # Corrigido: 'topic_message' para 'topicMessage'
  http_method             = aws_api_gateway_method.create_pagamento.http_method
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = aws_lambda_function.ms-communication-buytrip_lambda.invoke_arn
}

resource "aws_api_gateway_deployment" "ms-communication-buytrip_lambda_api_deployment" {
  depends_on  = [
    aws_api_gateway_integration.post_pagamentos_trip_integration
  ]
  rest_api_id = aws_api_gateway_rest_api.ms-communication-buytrip_lambda_api.id
  triggers = {
    redeployment = timestamp()
  }
  lifecycle {
    create_before_destroy = true
  }
}

# Definindo o estágio "prod" de forma explícita
resource "aws_api_gateway_stage" "prod_stage_buytrip" {
  deployment_id = aws_api_gateway_deployment.ms-communication-buytrip_lambda_api_deployment.id
  rest_api_id   = aws_api_gateway_rest_api.ms-communication-buytrip_lambda_api.id
  stage_name    = "prod"
}

# Permissão para o API Gateway invocar o Lambda
resource "aws_lambda_permission" "apigw_invoke_buytrip" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.ms-communication-buytrip_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  #source_arn    = "${aws_api_gateway_rest_api.ms-communication-buytrip_lambda_api.execution_arn}/*/*"
  source_arn = "${aws_api_gateway_rest_api.ms-communication-buytrip_lambda_api.execution_arn}/*"
}

# Criação do Lambda para Buyprocess
resource "aws_lambda_function" "ms-communication-buyprocess_lambda" {
  function_name = "StreamLambdaHandlerProcess"
  handler       = "br.com.lduran.StreamLambdaHandlerProcess::handleRequest"
  runtime       = "java11"
  memory_size   = 512
  timeout       = 30
  filename      = "ms-communication-buyprocess/target/ms-communication-buyprocess-0.0.1-SNAPSHOT-lambda-package.zip"
  role          = aws_iam_role.lambda_execution_role.arn

  environment {
    variables = {
      AWS_ENDPOINT_URL = "http://host.docker.internal:4566"
    }
  }
}

# Criação do trigger SQS para a Lambda Buyprocess
resource "aws_lambda_event_source_mapping" "sqs_to_buyprocess_lambda_trigger" {
  event_source_arn = "arn:aws:sqs:sa-east-1:000000000000:sqs_passagens"
  function_name    = aws_lambda_function.ms-communication-buyprocess_lambda.arn
  batch_size       = 1
  maximum_batching_window_in_seconds = 1
  enabled          = true
}