# Data Module - Essential Resources
# DynamoDB: Main tables (Events, TicketOrders, TicketInventory)
# SQS: Main queue and DLQ
# ElastiCache: Redis for cache

# ============================================================================
# DynamoDB Tables (Only essentials)
# ============================================================================

# Table: Events (Event Sourcing)
resource "aws_dynamodb_table" "events" {
  name         = "${var.environment}-Events"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "aggregateId"
  range_key    = "version"

  attribute {
    name = "aggregateId"
    type = "S"
  }
  attribute {
    name = "version"
    type = "N"
  }

  server_side_encryption {
    enabled = true
  }

  tags = var.tags
}

# Table: TicketOrders
resource "aws_dynamodb_table" "ticket_orders" {
  name         = "${var.environment}-TicketOrders"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "orderId"

  attribute {
    name = "orderId"
    type = "S"
  }

  server_side_encryption {
    enabled = true
  }

  tags = var.tags
}

# Table: TicketInventory
resource "aws_dynamodb_table" "ticket_inventory" {
  name         = "${var.environment}-TicketInventory"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"
  range_key    = "ticketType"

  attribute {
    name = "eventId"
    type = "S"
  }
  attribute {
    name = "ticketType"
    type = "S"
  }

  server_side_encryption {
    enabled = true
  }

  tags = var.tags
}

# ============================================================================
# SQS Queues (Only essentials)
# ============================================================================

# Main queue for orders
resource "aws_sqs_queue" "ticket_order" {
  name                       = "${var.environment}-ticket-order-queue"
  message_retention_seconds  = 345600
  visibility_timeout_seconds = 30

  tags = var.tags
}

# Dead Letter Queue
resource "aws_sqs_queue" "ticket_dlq" {
  name                      = "${var.environment}-ticket-dlq"
  message_retention_seconds = 1209600

  tags = var.tags
}

# Configure DLQ
resource "aws_sqs_queue_redrive_policy" "ticket_order" {
  queue_url = aws_sqs_queue.ticket_order.id
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.ticket_dlq.arn
    maxReceiveCount     = 3
  })
}

# ============================================================================
# ElastiCache Redis (Simplified)
# ============================================================================

resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.environment}-redis-subnet-group"
  subnet_ids = var.private_subnet_ids
  tags       = var.tags
}

# Redis Cluster (simplified - single node in dev, multi-AZ in prod)
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${var.environment}-redis"
  description          = "Redis for EventTicket"
  engine               = "redis"
  engine_version       = "7.0"
  node_type           = "cache.t3.micro"
  port                = 6379
  subnet_group_name   = aws_elasticache_subnet_group.redis.name
  security_group_ids  = var.security_group_ids
  
  # Simplified: Multi-AZ only in production
  automatic_failover_enabled = var.environment == "prod" ? true : false
  num_cache_clusters         = var.environment == "prod" ? 2 : 1

  at_rest_encryption_enabled = true

  tags = var.tags
}
