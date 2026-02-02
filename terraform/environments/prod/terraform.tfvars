# ============================================================================
# Configuración para entorno PRODUCCIÓN
# ============================================================================

environment = "prod"
aws_region  = "us-east-1"

# Aplicación
app_name  = "eventticket"
app_image = "123456789012.dkr.ecr.us-east-1.amazonaws.com/eventticket:latest"
app_port  = 8080

# Networking
vpc_cidr           = "10.2.0.0/16"
enable_nat_gateway = true
single_nat_gateway = false  # NAT Gateway por AZ para máxima disponibilidad

# ECS Configuration
ecs_cpu          = 1024  # 1 vCPU
ecs_memory       = 2048  # 2 GB
ecs_desired_count = 3    # Mínimo 3 instancias para alta disponibilidad
ecs_min_capacity = 3
ecs_max_capacity = 20    # Escalado agresivo para picos de tráfico

# Logging
log_retention_days = 30  # Mayor retención en producción
