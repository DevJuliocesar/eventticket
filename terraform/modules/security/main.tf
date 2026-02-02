# Módulo de Seguridad
# Security Groups, IAM Roles, Encryption

# Security Groups

# Security Group para ALB (Application Load Balancer)
resource "aws_security_group" "alb" {
  name        = "${var.environment}-alb-sg"
  description = "Security group para Application Load Balancer"
  vpc_id      = var.vpc_id

  # HTTP desde internet
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS desde internet
  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Todo el tráfico saliente permitido
  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.environment}-alb-sg"
    }
  )
}

# Security Group para ECS Tasks
resource "aws_security_group" "ecs" {
  name        = "${var.environment}-ecs-sg"
  description = "Security group para ECS tasks"
  vpc_id      = var.vpc_id

  # Solo tráfico desde ALB
  ingress {
    description     = "HTTP from ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Tráfico saliente permitido (para DynamoDB, SQS, Redis)
  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.environment}-ecs-sg"
    }
  )
}

# Security Group para ElastiCache Redis
resource "aws_security_group" "redis" {
  name        = "${var.environment}-redis-sg"
  description = "Security group para ElastiCache Redis"
  vpc_id      = var.vpc_id

  # Solo tráfico desde ECS tasks
  ingress {
    description     = "Redis from ECS"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  # Sin tráfico saliente (Redis no necesita salida)
  egress {
    description = "No outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = []
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.environment}-redis-sg"
    }
  )
}

# IAM Roles

# IAM Role para ECS Task Execution (pull images, write logs)
resource "aws_iam_role" "ecs_task_execution" {
  name = "${var.environment}-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

# IAM Policy para ECS Task Execution
resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# IAM Role para ECS Task (runtime de la aplicación)
resource "aws_iam_role" "ecs_task" {
  name = "${var.environment}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

# IAM Policy para acceso a DynamoDB
resource "aws_iam_policy" "dynamodb_access" {
  name        = "${var.environment}-dynamodb-access"
  description = "Permite acceso a tablas DynamoDB del proyecto"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Query",
          "dynamodb:Scan",
          "dynamodb:BatchGetItem",
          "dynamodb:BatchWriteItem"
        ]
        Resource = [
          "arn:aws:dynamodb:${var.aws_region}:${var.aws_account_id}:table/${var.environment}-*",
          "arn:aws:dynamodb:${var.aws_region}:${var.aws_account_id}:table/${var.environment}-*/index/*"
        ]
      }
    ]
  })

  tags = var.tags
}

# IAM Policy para acceso a SQS
resource "aws_iam_policy" "sqs_access" {
  name        = "${var.environment}-sqs-access"
  description = "Permite acceso a colas SQS del proyecto"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl"
        ]
        Resource = [
          "arn:aws:sqs:${var.aws_region}:${var.aws_account_id}:${var.environment}-*"
        ]
      }
    ]
  })

  tags = var.tags
}

# Attach policies al task role
resource "aws_iam_role_policy_attachment" "ecs_task_dynamodb" {
  role       = aws_iam_role.ecs_task.name
  policy_arn = aws_iam_policy.dynamodb_access.arn
}

resource "aws_iam_role_policy_attachment" "ecs_task_sqs" {
  role       = aws_iam_role.ecs_task.name
  policy_arn = aws_iam_policy.sqs_access.arn
}

# KMS Keys simplificadas (usando AWS managed keys por defecto)
