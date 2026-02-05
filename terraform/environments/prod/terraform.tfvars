# ============================================================================
# Configuration for PRODUCTION environment
# ============================================================================

environment = "prod"
aws_region  = "us-east-1"

# Application
app_name = "eventticket"
app_port = 8080

# Networking
vpc_cidr           = "10.2.0.0/16"
enable_nat_gateway = true
single_nat_gateway = false  # NAT Gateway per AZ for maximum availability

# App Runner Configuration
apprunner_cpu           = "1 vCPU"
apprunner_memory        = "2 GB"
apprunner_min_size      = 3
apprunner_max_size      = 20
apprunner_max_concurrency = 200
apprunner_auto_deploy   = true

# Logging
log_retention_days = 30  # Higher retention in production
