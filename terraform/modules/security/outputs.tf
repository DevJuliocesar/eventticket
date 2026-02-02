output "alb_security_group_id" {
  description = "ID del Security Group del ALB"
  value       = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  description = "ID del Security Group de ECS"
  value       = aws_security_group.ecs.id
}

output "redis_security_group_id" {
  description = "ID del Security Group de Redis"
  value       = aws_security_group.redis.id
}

output "ecs_task_execution_role_arn" {
  description = "ARN del IAM role para ECS task execution"
  value       = aws_iam_role.ecs_task_execution.arn
}

output "ecs_task_role_arn" {
  description = "ARN del IAM role para ECS task"
  value       = aws_iam_role.ecs_task.arn
}

