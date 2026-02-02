# ============================================================================
# Configuración para entorno DEV
# ============================================================================

environment = "dev"
aws_region  = "us-east-1"

# Aplicación
app_name  = "eventticket"
app_image = "123456789012.dkr.ecr.us-east-1.amazonaws.com/eventticket:dev"
app_port  = 8080

# Networking
vpc_cidr           = "10.0.0.0/16"
enable_nat_gateway = true
single_nat_gateway = true  # Reducir costos en dev

# ECS Configuration
ecs_cpu          = 256   # 0.25 vCPU (suficiente para dev)
ecs_memory       = 512   # 512 MB
ecs_desired_count = 1    # Una sola instancia en dev
ecs_min_capacity = 1
ecs_max_capacity = 3     # Escalado limitado en dev

# Logging
log_retention_days = 3   # Menos retención en dev
