output "dynamodb_tables" {
  description = "Created DynamoDB tables"
  value = {
    events          = aws_dynamodb_table.events.name
    ticket_orders   = aws_dynamodb_table.ticket_orders.name
    ticket_inventory = aws_dynamodb_table.ticket_inventory.name
  }
}

output "sqs_queues" {
  description = "Created SQS queues"
  value = {
    ticket_order = aws_sqs_queue.ticket_order.url
    ticket_dlq   = aws_sqs_queue.ticket_dlq.url
  }
}

output "redis_endpoint" {
  description = "Redis endpoint"
  value       = aws_elasticache_replication_group.redis.configuration_endpoint_address
  sensitive   = true
}
