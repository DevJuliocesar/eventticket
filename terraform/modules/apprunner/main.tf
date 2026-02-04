# Módulo App Runner
# AWS App Runner para despliegue simplificado de contenedores

# ECR Repository (si no existe)
data "aws_caller_identity" "current" {}

resource "aws_ecr_repository" "app" {
  name                 = "${var.environment}-${var.app_name}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.environment}-${var.app_name}-ecr"
    }
  )
}

# ECR Lifecycle Policy (mantener últimas 10 imágenes)
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# IAM Role para App Runner Access (para que App Runner acceda a ECR y otros servicios)
resource "aws_iam_role" "apprunner_access" {
  name = "${var.environment}-${var.app_name}-apprunner-access-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "build.apprunner.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.tags
}

# IAM Policy para acceso a ECR (para build)
resource "aws_iam_role_policy" "apprunner_ecr_access" {
  name = "${var.environment}-${var.app_name}-apprunner-ecr-policy"
  role = aws_iam_role.apprunner_access.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage"
        ]
        Resource = aws_ecr_repository.app.arn
      }
    ]
  })
}

# IAM Role para App Runner Instance (runtime)
resource "aws_iam_role" "apprunner_instance" {
  name = "${var.environment}-${var.app_name}-apprunner-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "tasks.apprunner.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.tags
}

# IAM Policy para App Runner Instance (acceso a DynamoDB, SQS, ElastiCache, Logs)
resource "aws_iam_role_policy" "apprunner_instance" {
  name = "${var.environment}-${var.app_name}-apprunner-instance-policy"
  role = aws_iam_role.apprunner_instance.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:*"
        ]
        Resource = [
          for table_name in values(var.dynamodb_tables) : "arn:aws:dynamodb:${var.aws_region}:${data.aws_caller_identity.current.account_id}:table/${table_name}"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "sqs:*"
        ]
        Resource = [
          for queue_url in values(var.sqs_queues) : "arn:aws:sqs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "elasticache:DescribeCacheClusters",
          "elasticache:DescribeReplicationGroups"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:log-group:/aws/apprunner/*"
      }
    ]
  })
}

# CloudWatch Log Group para App Runner
resource "aws_cloudwatch_log_group" "apprunner" {
  name              = "/aws/apprunner/${var.environment}/${var.app_name}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name = "${var.environment}-${var.app_name}-apprunner-logs"
    }
  )
}

# Auto Scaling Configuration (opcional, App Runner tiene auto-scaling por defecto)
resource "aws_apprunner_auto_scaling_configuration_version" "app" {
  auto_scaling_configuration_name = "${var.environment}-${var.app_name}-autoscaling"

  max_concurrency = var.max_concurrency
  max_size        = var.max_size
  min_size        = var.min_size

  tags = var.tags
}

# App Runner Service
resource "aws_apprunner_service" "app" {
  service_name = "${var.environment}-${var.app_name}"

  source_configuration {
    image_repository {
      image_identifier      = "${aws_ecr_repository.app.repository_url}:latest"
      image_configuration {
        port = var.app_port
        runtime_environment_variables = merge(
          {
            SPRING_PROFILES_ACTIVE = var.environment
            AWS_REGION            = var.aws_region
            SPRING_DATA_REDIS_HOST = var.redis_endpoint
            AWS_DYNAMODB_ENDPOINT  = ""
            AWS_SQS_ENDPOINT       = ""
          },
          var.extra_environment_variables
        )
      }
      image_repository_type = "ECR"
    }
    auto_deployments_enabled = var.auto_deploy
    access_role_arn         = aws_iam_role.apprunner_access.arn
  }

  instance_configuration {
    cpu    = var.cpu
    memory = var.memory
    instance_role_arn = aws_iam_role.apprunner_instance.arn
  }

  health_check_configuration {
    protocol            = "HTTP"
    path                = var.health_check_path
    interval            = var.health_check_interval
    timeout             = var.health_check_timeout
    healthy_threshold   = var.health_check_healthy_threshold
    unhealthy_threshold = var.health_check_unhealthy_threshold
  }

  auto_scaling_configuration_arn = aws_apprunner_auto_scaling_configuration_version.app.arn

  tags = merge(
    var.tags,
    {
      Name = "${var.environment}-${var.app_name}"
    }
  )
}
