# ============================================================================
# Configuration Variables
# ============================================================================

variable "aws_region" {
  description = "AWS region where infrastructure will be deployed"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be: dev, staging, or prod"
  }
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "eventticket"
}

variable "app_port" {
  description = "Application port"
  type        = number
  default     = 8080
}

# ============================================================================
# Networking Variables
# ============================================================================

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "enable_nat_gateway" {
  description = "Enable NAT Gateway for private subnets"
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Use a single NAT Gateway for all AZs (reduces costs)"
  type        = bool
  default     = false
}

# ============================================================================
# App Runner Configuration
# ============================================================================

variable "apprunner_cpu" {
  description = "CPU for App Runner (0.25 vCPU, 0.5 vCPU, 1 vCPU, 2 vCPU, 4 vCPU)"
  type        = string
  default     = "0.5 vCPU"
}

variable "apprunner_memory" {
  description = "Memory for App Runner (0.5 GB, 1 GB, 2 GB, 3 GB, 4 GB, 6 GB, 8 GB, 12 GB)"
  type        = string
  default     = "1 GB"
}

variable "apprunner_min_size" {
  description = "Minimum number of App Runner instances"
  type        = number
  default     = 1
}

variable "apprunner_max_size" {
  description = "Maximum number of App Runner instances"
  type        = number
  default     = 10
}

variable "apprunner_max_concurrency" {
  description = "Maximum concurrency per App Runner instance"
  type        = number
  default     = 100
}

variable "apprunner_auto_deploy" {
  description = "Enable auto-deploy when image is updated in ECR"
  type        = bool
  default     = true
}

# ============================================================================
# Logging
# ============================================================================

variable "log_retention_days" {
  description = "CloudWatch log retention days"
  type        = number
  default     = 7
}

# ============================================================================
# Local Values
# ============================================================================

locals {
  common_tags = {
    Project     = "EventTicket"
    Environment = var.environment
    ManagedBy   = "Terraform"
    Application = var.app_name
  }
}
