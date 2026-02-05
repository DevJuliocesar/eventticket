# EventTicket - Terraform Infrastructure
# Orchestrates modules: Networking, Security, Data, App Runner

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend configuration (uncomment and configure for production)
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

# Modules

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

# Application - App Runner
module "apprunner" {
  source = "./modules/apprunner"
  
  environment = var.environment
  app_name    = var.app_name
  app_port    = var.app_port
  aws_region  = var.aws_region
  
  cpu    = var.apprunner_cpu
  memory = var.apprunner_memory
  
  redis_endpoint  = module.data.redis_endpoint
  dynamodb_tables = module.data.dynamodb_tables
  sqs_queues      = module.data.sqs_queues
  
  min_size        = var.apprunner_min_size
  max_size        = var.apprunner_max_size
  max_concurrency = var.apprunner_max_concurrency
  
  health_check_path = "/actuator/health"
  log_retention_days = var.log_retention_days
  auto_deploy      = var.apprunner_auto_deploy
  
  tags = local.common_tags
}

# Outputs

output "vpc_id" {
  description = "VPC ID"
  value       = module.networking.vpc_id
}

# App Runner Outputs
output "apprunner_service_url" {
  description = "App Runner service URL"
  value       = module.apprunner.service_url
}

output "apprunner_service_arn" {
  description = "App Runner service ARN"
  value       = module.apprunner.service_arn
}

output "ecr_repository_url" {
  description = "ECR repository URL for Docker image push"
  value       = module.apprunner.ecr_repository_url
}

output "redis_endpoint" {
  description = "ElastiCache Redis endpoint"
  value       = module.data.redis_endpoint
  sensitive   = true
}

output "dynamodb_tables" {
  description = "Map of created DynamoDB tables"
  value       = module.data.dynamodb_tables
}

output "sqs_queues" {
  description = "Map of created SQS queues"
  value       = module.data.sqs_queues
}
