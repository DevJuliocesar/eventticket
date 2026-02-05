variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "app_name" {
  description = "Application name"
  type        = string
}

variable "app_port" {
  description = "Application port"
  type        = number
  default     = 8080
}

variable "cpu" {
  description = "CPU para App Runner (0.25 vCPU, 0.5 vCPU, 1 vCPU, 2 vCPU, 4 vCPU)"
  type        = string
  default     = "0.5 vCPU"
}

variable "memory" {
  description = "Memory for App Runner (0.5 GB, 1 GB, 2 GB, 3 GB, 4 GB, 6 GB, 8 GB, 12 GB)"
  type        = string
  default     = "1 GB"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "redis_endpoint" {
  description = "Redis endpoint (ElastiCache)"
  type        = string
  sensitive   = true
}

variable "dynamodb_tables" {
  description = "Map of DynamoDB table names"
  type        = map(string)
}

variable "sqs_queues" {
  description = "Map of SQS queue URLs"
  type        = map(string)
}

variable "log_retention_days" {
  description = "CloudWatch log retention days"
  type        = number
  default     = 7
}

variable "health_check_path" {
  description = "Health check path"
  type        = string
  default     = "/actuator/health"
}

variable "health_check_interval" {
  description = "Health check interval in seconds"
  type        = number
  default     = 10
}

variable "health_check_timeout" {
  description = "Health check timeout in seconds"
  type        = number
  default     = 5
}

variable "health_check_healthy_threshold" {
  description = "Healthy threshold for health checks"
  type        = number
  default     = 1
}

variable "health_check_unhealthy_threshold" {
  description = "Unhealthy threshold for health checks"
  type        = number
  default     = 5
}

variable "auto_deploy" {
  description = "Enable auto-deploy when image is updated"
  type        = bool
  default     = true
}

variable "max_concurrency" {
  description = "Maximum concurrency per instance"
  type        = number
  default     = 100
}

variable "max_size" {
  description = "Maximum number of instances"
  type        = number
  default     = 10
}

variable "min_size" {
  description = "Minimum number of instances"
  type        = number
  default     = 1
}

variable "extra_environment_variables" {
  description = "Extra environment variables"
  type        = map(string)
  default     = {}
}

variable "tags" {
  description = "Common tags for all resources"
  type        = map(string)
  default     = {}
}
