variable "environment" {
  description = "Entorno de despliegue"
  type        = string
}

variable "vpc_id" {
  description = "ID de la VPC"
  type        = string
}

variable "public_subnet_ids" {
  description = "IDs de las subnets públicas"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "IDs de las subnets privadas"
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "ID del Security Group del ALB"
  type        = string
}

variable "ecs_security_group_id" {
  description = "ID del Security Group de ECS"
  type        = string
}

variable "task_execution_role_arn" {
  description = "ARN del IAM role para task execution"
  type        = string
}

variable "task_role_arn" {
  description = "ARN del IAM role para task"
  type        = string
}

variable "app_name" {
  description = "Nombre de la aplicación"
  type        = string
}

variable "app_image" {
  description = "URI de la imagen Docker"
  type        = string
}

variable "app_port" {
  description = "Puerto de la aplicación"
  type        = number
}

variable "cpu" {
  description = "CPU units para el task"
  type        = number
}

variable "memory" {
  description = "Memoria (MB) para el task"
  type        = number
}

variable "desired_count" {
  description = "Número deseado de tareas"
  type        = number
}

variable "min_capacity" {
  description = "Capacidad mínima para Auto Scaling"
  type        = number
}

variable "max_capacity" {
  description = "Capacidad máxima para Auto Scaling"
  type        = number
}

variable "aws_region" {
  description = "Región AWS"
  type        = string
}

variable "redis_endpoint" {
  description = "Endpoint de Redis"
  type        = string
  sensitive   = true
}

variable "dynamodb_tables" {
  description = "Mapa de tablas DynamoDB"
  type        = map(string)
}

variable "sqs_queues" {
  description = "Mapa de colas SQS"
  type        = map(string)
}

variable "log_retention_days" {
  description = "Días de retención de logs"
  type        = number
}

variable "tags" {
  description = "Tags comunes para todos los recursos"
  type        = map(string)
  default     = {}
}
