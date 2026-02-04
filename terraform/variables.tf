# ============================================================================
# Variables de Configuración
# ============================================================================

variable "aws_region" {
  description = "Región AWS donde se desplegará la infraestructura"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Entorno de despliegue (dev, staging, prod)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "El entorno debe ser: dev, staging o prod"
  }
}

variable "app_name" {
  description = "Nombre de la aplicación"
  type        = string
  default     = "eventticket"
}

variable "app_image" {
  description = "URI de la imagen Docker (ECR o Docker Hub)"
  type        = string
  default     = "eventticket-system:latest"
}

variable "app_port" {
  description = "Puerto de la aplicación"
  type        = number
  default     = 8080
}

# ============================================================================
# Networking Variables
# ============================================================================

variable "vpc_cidr" {
  description = "CIDR block para la VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "enable_nat_gateway" {
  description = "Habilitar NAT Gateway para subnets privadas"
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Usar un solo NAT Gateway para todas las AZs (reduce costos)"
  type        = bool
  default     = false
}

# ============================================================================
# App Runner Configuration (Nuevo - Migrado de ECS)
# ============================================================================

variable "apprunner_cpu" {
  description = "CPU para App Runner (0.25 vCPU, 0.5 vCPU, 1 vCPU, 2 vCPU, 4 vCPU)"
  type        = string
  default     = "0.5 vCPU"
}

variable "apprunner_memory" {
  description = "Memoria para App Runner (0.5 GB, 1 GB, 2 GB, 3 GB, 4 GB, 6 GB, 8 GB, 12 GB)"
  type        = string
  default     = "1 GB"
}

variable "apprunner_min_size" {
  description = "Número mínimo de instancias App Runner"
  type        = number
  default     = 1
}

variable "apprunner_max_size" {
  description = "Número máximo de instancias App Runner"
  type        = number
  default     = 10
}

variable "apprunner_max_concurrency" {
  description = "Máxima concurrencia por instancia App Runner"
  type        = number
  default     = 100
}

variable "apprunner_auto_deploy" {
  description = "Habilitar auto-deploy cuando se actualiza la imagen en ECR"
  type        = bool
  default     = true
}

# ============================================================================
# ECS Configuration (Comentado - Mantener para referencia o rollback)
# ============================================================================

# variable "ecs_cpu" {
#   description = "CPU units para el task definition (256 = 0.25 vCPU, 512 = 0.5 vCPU, 1024 = 1 vCPU)"
#   type        = number
#   default     = 512
# }
# 
# variable "ecs_memory" {
#   description = "Memoria (MB) para el task definition"
#   type        = number
#   default     = 1024
# }
# 
# variable "ecs_desired_count" {
#   description = "Número deseado de tareas ECS"
#   type        = number
#   default     = 2
# }
# 
# variable "ecs_min_capacity" {
#   description = "Capacidad mínima para Auto Scaling"
#   type        = number
#   default     = 1
# }
# 
# variable "ecs_max_capacity" {
#   description = "Capacidad máxima para Auto Scaling"
#   type        = number
#   default     = 10
# }

# ============================================================================
# Logging
# ============================================================================

variable "log_retention_days" {
  description = "Días de retención de logs en CloudWatch"
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
