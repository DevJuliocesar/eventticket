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

# App Runner Configuration (Migrado de ECS)
apprunner_cpu           = "0.5 vCPU"  # Mínimo para App Runner
apprunner_memory        = "1 GB"      # Mínimo para App Runner
apprunner_min_size      = 1
apprunner_max_size      = 3           # Escalado limitado en dev
apprunner_max_concurrency = 50       # Menor concurrencia en dev
apprunner_auto_deploy   = true

# ECS Configuration (Comentado - Mantener para referencia o rollback)
# ecs_cpu          = 256   # 0.25 vCPU (suficiente para dev)
# ecs_memory       = 512   # 512 MB
# ecs_desired_count = 1    # Una sola instancia en dev
# ecs_min_capacity = 1
# ecs_max_capacity = 3     # Escalado limitado en dev

# Logging
log_retention_days = 3   # Menos retención en dev
