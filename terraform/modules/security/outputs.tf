output "redis_security_group_id" {
  description = "Redis Security Group ID"
  value       = aws_security_group.redis.id
}

