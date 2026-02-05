output "service_url" {
  description = "App Runner service URL"
  value       = aws_apprunner_service.app.service_url
}

output "service_arn" {
  description = "App Runner service ARN"
  value       = aws_apprunner_service.app.arn
}

output "service_id" {
  description = "App Runner service ID"
  value       = aws_apprunner_service.app.id
}

output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.app.repository_url
}

output "ecr_repository_arn" {
  description = "ECR repository ARN"
  value       = aws_ecr_repository.app.arn
}

output "apprunner_access_role_arn" {
  description = "App Runner access IAM role ARN (for ECR)"
  value       = aws_iam_role.apprunner_access.arn
}

output "apprunner_instance_role_arn" {
  description = "App Runner instance IAM role ARN (runtime)"
  value       = aws_iam_role.apprunner_instance.arn
}

output "autoscaling_configuration_arn" {
  description = "Auto-scaling configuration ARN"
  value       = aws_apprunner_auto_scaling_configuration_version.app.arn
}
