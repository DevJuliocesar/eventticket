variable "environment" {
  description = "Entorno de despliegue"
  type        = string
}

variable "private_subnet_ids" {
  description = "IDs de las subnets privadas"
  type        = list(string)
}

variable "security_group_ids" {
  description = "IDs de los Security Groups para Redis"
  type        = list(string)
}

variable "tags" {
  description = "Tags comunes"
  type        = map(string)
  default     = {}
}
