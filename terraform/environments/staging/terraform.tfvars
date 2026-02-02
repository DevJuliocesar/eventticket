# ============================================================================
# Configuración para entorno STAGING
# ============================================================================

environment = "staging"
aws_region  = "us-east-1"

# Aplicación
app_name  = "eventticket"
app_image = "123456789012.dkr.ecr.us-east-1.amazonaws.com/eventticket:staging"
app_port  = 8080

# Networking
vpc_cidr           = "10.1.0.0/16"
enable_nat_gateway = true
single_nat_gateway = false  # Multi-AZ para staging

# ECS Configuration
ecs_cpu          = 512   # 0.5 vCPU
ecs_memory       = 1024  # 1 GB
ecs_desired_count = 2    # Dos instancias para alta disponibilidad
ecs_min_capacity = 2
ecs_max_capacity = 5

# Logging
log_retention_days = 7
