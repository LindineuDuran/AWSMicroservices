echo ### Cria Tabela Cartao - Usar o Git Bash
aws --endpoint-url=http://localhost:4566 --profile localstack dynamodb create-table --table-name Cartao --attribute-definitions   AttributeName=id,AttributeType=S   AttributeName=nro_cartao,AttributeType=S   AttributeName=codigo_seguranca_cartao,AttributeType=N --key-schema   AttributeName=id,KeyType=HASH --global-secondary-indexes "[{\"IndexName\":\"CartaoIndex\",\"KeySchema\":[{\"AttributeName\":\"nro_cartao\",\"KeyType\":\"HASH\"},{\"AttributeName\":\"codigo_seguranca_cartao\",\"KeyType\":\"RANGE\"}],\"Projection\":{\"ProjectionType\":\"ALL\"},\"ProvisionedThroughput\":{\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}}]" --provisioned-throughput   ReadCapacityUnits=5,WriteCapacityUnits=5

echo ### Cria Tabela Pagamento
aws --endpoint-url=http://localhost:4566 --profile localstack dynamodb create-table --table-name Pagamento --attribute-definitions AttributeName=id,AttributeType=S --key-schema AttributeName=id,KeyType=HASH  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
