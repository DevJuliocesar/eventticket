# ============================================================================
# Configuration for DEV environment
# ============================================================================

environment = "dev"
aws_region  = "us-east-1"

# Application
app_name = "eventticket"
app_port = 8080

# Networking
vpc_cidr           = "10.0.0.0/16"
enable_nat_gateway = true
single_nat_gateway = true  # Reduce costs in dev

# App Runner Configuration
apprunner_cpu           = "0.5 vCPU"
apprunner_memory        = "1 GB"
apprunner_min_size      = 1
apprunner_max_size      = 3
apprunner_max_concurrency = 50
apprunner_auto_deploy   = true

# Logging
log_retention_days = 3   # Less retention in dev
