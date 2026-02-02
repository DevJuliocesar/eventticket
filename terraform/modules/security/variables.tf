variable "environment" {
  description = "Entorno de despliegue"
  type        = string
}

variable "vpc_id" {
  description = "ID de la VPC"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block de la VPC"
  type        = string
}

variable "private_subnet_ids" {
  description = "IDs de las subnets privadas"
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "IDs de las subnets públicas"
  type        = list(string)
}

variable "aws_account_id" {
  description = "ID de la cuenta AWS"
  type        = string
}

variable "aws_region" {
  description = "Región AWS"
  type        = string
}

variable "tags" {
  description = "Tags comunes para todos los recursos"
  type        = map(string)
  default     = {}
}
