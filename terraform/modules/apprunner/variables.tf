variable "environment" {
  description = "Entorno de despliegue"
  type        = string
}

variable "app_name" {
  description = "Nombre de la aplicación"
  type        = string
}

variable "app_port" {
  description = "Puerto de la aplicación"
  type        = number
  default     = 8080
}

variable "cpu" {
  description = "CPU para App Runner (0.25 vCPU, 0.5 vCPU, 1 vCPU, 2 vCPU, 4 vCPU)"
  type        = string
  default     = "0.5 vCPU"
}

variable "memory" {
  description = "Memoria para App Runner (0.5 GB, 1 GB, 2 GB, 3 GB, 4 GB, 6 GB, 8 GB, 12 GB)"
  type        = string
  default     = "1 GB"
}

variable "aws_region" {
  description = "Región AWS"
  type        = string
}

variable "redis_endpoint" {
  description = "Endpoint de Redis (ElastiCache)"
  type        = string
  sensitive   = true
}

variable "dynamodb_tables" {
  description = "Mapa de nombres de tablas DynamoDB"
  type        = map(string)
}

variable "sqs_queues" {
  description = "Mapa de URLs de colas SQS"
  type        = map(string)
}

variable "log_retention_days" {
  description = "Días de retención de logs en CloudWatch"
  type        = number
  default     = 7
}

variable "health_check_path" {
  description = "Ruta para health check"
  type        = string
  default     = "/actuator/health"
}

variable "health_check_interval" {
  description = "Intervalo de health check en segundos"
  type        = number
  default     = 10
}

variable "health_check_timeout" {
  description = "Timeout de health check en segundos"
  type        = number
  default     = 5
}

variable "health_check_healthy_threshold" {
  description = "Umbral de health checks exitosos"
  type        = number
  default     = 1
}

variable "health_check_unhealthy_threshold" {
  description = "Umbral de health checks fallidos"
  type        = number
  default     = 5
}

variable "auto_deploy" {
  description = "Habilitar auto-deploy cuando se actualiza la imagen"
  type        = bool
  default     = true
}

variable "max_concurrency" {
  description = "Máxima concurrencia por instancia"
  type        = number
  default     = 100
}

variable "max_size" {
  description = "Número máximo de instancias"
  type        = number
  default     = 10
}

variable "min_size" {
  description = "Número mínimo de instancias"
  type        = number
  default     = 1
}

variable "extra_environment_variables" {
  description = "Variables de entorno adicionales"
  type        = map(string)
  default     = {}
}

variable "tags" {
  description = "Tags comunes para todos los recursos"
  type        = map(string)
  default     = {}
}
