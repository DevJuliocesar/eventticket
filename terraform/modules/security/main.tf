# Security Module
# Security Group for ElastiCache Redis

# Security Group for ElastiCache Redis
# Allows access from resources within VPC (App Runner can access)
resource "aws_security_group" "redis" {
  name        = "${var.environment}-redis-sg"
  description = "Security group for ElastiCache Redis"
  vpc_id      = var.vpc_id

  # Redis traffic from resources within VPC
  ingress {
    description = "Redis from VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # No outbound traffic (Redis doesn't need egress)
  egress {
    description = "No outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = []
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.environment}-redis-sg"
    }
  )
}
