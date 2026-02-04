output "service_url" {
  description = "URL del servicio App Runner"
  value       = aws_apprunner_service.app.service_url
}

output "service_arn" {
  description = "ARN del servicio App Runner"
  value       = aws_apprunner_service.app.arn
}

output "service_id" {
  description = "ID del servicio App Runner"
  value       = aws_apprunner_service.app.id
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR"
  value       = aws_ecr_repository.app.repository_url
}

output "ecr_repository_arn" {
  description = "ARN del repositorio ECR"
  value       = aws_ecr_repository.app.arn
}

output "apprunner_access_role_arn" {
  description = "ARN del IAM role de acceso de App Runner (para ECR)"
  value       = aws_iam_role.apprunner_access.arn
}

output "apprunner_instance_role_arn" {
  description = "ARN del IAM role de instancia de App Runner (runtime)"
  value       = aws_iam_role.apprunner_instance.arn
}

output "autoscaling_configuration_arn" {
  description = "ARN de la configuración de auto-scaling"
  value       = aws_apprunner_auto_scaling_configuration_version.app.arn
}
