# EventTicket - Infraestructura Terraform
# Orquesta módulos: Networking, Security, Data, Application

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend configuration (descomentar y configurar para producción)
  # backend "s3" {
  #   bucket         = "eventticket-terraform-state"
  #   key            = "terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "terraform-state-lock"
  # }
}

# Provider configuration
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "EventTicket"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Application = "eventticket-system"
    }
  }
}

# Data Sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

# Módulos

# Networking
module "networking" {
  source = "./modules/networking"
  environment = var.environment
  vpc_cidr = var.vpc_cidr
  azs = slice(data.aws_availability_zones.available.names, 0, 2)
  enable_nat_gateway = var.enable_nat_gateway
  single_nat_gateway = var.single_nat_gateway
  tags = local.common_tags
}

# Security
module "security" {
  source = "./modules/security"
  environment = var.environment
  vpc_id = module.networking.vpc_id
  vpc_cidr = module.networking.vpc_cidr
  private_subnet_ids = module.networking.private_subnet_ids
  public_subnet_ids = module.networking.public_subnet_ids
  aws_account_id = data.aws_caller_identity.current.account_id
  aws_region = var.aws_region
  tags = local.common_tags
}

# Data
module "data" {
  source = "./modules/data"
  environment = var.environment
  private_subnet_ids = module.networking.private_subnet_ids
  security_group_ids = [module.security.redis_security_group_id]
  tags = local.common_tags
}

# Application - App Runner (migrado de Fargate)
module "apprunner" {
  source = "./modules/apprunner"
  
  environment = var.environment
  app_name    = var.app_name
  app_port    = var.app_port
  aws_region  = var.aws_region
  
  # CPU y memoria convertidos a formato App Runner
  cpu    = var.apprunner_cpu
  memory = var.apprunner_memory
  
  # Configuración de servicios
  redis_endpoint  = module.data.redis_endpoint
  dynamodb_tables = module.data.dynamodb_tables
  sqs_queues      = module.data.sqs_queues
  
  # Auto-scaling
  min_size = var.apprunner_min_size
  max_size = var.apprunner_max_size
  max_concurrency = var.apprunner_max_concurrency
  
  # Health check
  health_check_path = "/actuator/health"
  
  # Logging
  log_retention_days = var.log_retention_days
  
  # Auto-deploy
  auto_deploy = var.apprunner_auto_deploy
  
  tags = local.common_tags
}

# Application - Fargate (comentado, mantener para referencia o rollback)
# module "application" {
#   source = "./modules/application"
#   environment = var.environment
#   vpc_id = module.networking.vpc_id
#   public_subnet_ids = module.networking.public_subnet_ids
#   private_subnet_ids = module.networking.private_subnet_ids
#   alb_security_group_id = module.security.alb_security_group_id
#   ecs_security_group_id = module.security.ecs_security_group_id
#   task_execution_role_arn = module.security.ecs_task_execution_role_arn
#   task_role_arn = module.security.ecs_task_role_arn
#   app_name = var.app_name
#   app_image = var.app_image
#   app_port = var.app_port
#   cpu = var.ecs_cpu
#   memory = var.ecs_memory
#   desired_count = var.ecs_desired_count
#   min_capacity = var.ecs_min_capacity
#   max_capacity = var.ecs_max_capacity
#   aws_region = var.aws_region
#   redis_endpoint = module.data.redis_endpoint
#   dynamodb_tables = module.data.dynamodb_tables
#   sqs_queues = module.data.sqs_queues
#   log_retention_days = var.log_retention_days
#   tags = local.common_tags
# }

# Outputs

output "vpc_id" {
  description = "ID de la VPC"
  value       = module.networking.vpc_id
}

# App Runner Outputs
output "apprunner_service_url" {
  description = "URL del servicio App Runner"
  value       = module.apprunner.service_url
}

output "apprunner_service_arn" {
  description = "ARN del servicio App Runner"
  value       = module.apprunner.service_arn
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR para push de imágenes"
  value       = module.apprunner.ecr_repository_url
}

# Fargate Outputs (comentados, mantener para referencia)
# output "alb_dns_name" {
#   description = "DNS name del Application Load Balancer"
#   value       = module.application.alb_dns_name
# }
# 
# output "alb_zone_id" {
#   description = "Zone ID del ALB para Route53"
#   value       = module.application.alb_zone_id
# }
# 
# output "ecs_cluster_name" {
#   description = "Nombre del cluster ECS"
#   value       = module.application.ecs_cluster_name
# }
# 
# output "ecs_service_name" {
#   description = "Nombre del servicio ECS"
#   value       = module.application.ecs_service_name
# }

output "redis_endpoint" {
  description = "Endpoint de ElastiCache Redis"
  value       = module.data.redis_endpoint
  sensitive   = true
}

output "dynamodb_tables" {
  description = "Mapa de tablas DynamoDB creadas"
  value       = module.data.dynamodb_tables
}

output "sqs_queues" {
  description = "Mapa de colas SQS creadas"
  value       = module.data.sqs_queues
}
