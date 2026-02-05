# ============================================================================
# Configuration for STAGING environment
# ============================================================================

environment = "staging"
aws_region  = "us-east-1"

# Application
app_name = "eventticket"
app_port = 8080

# Networking
vpc_cidr           = "10.1.0.0/16"
enable_nat_gateway = true
single_nat_gateway = false  # Multi-AZ for staging

# App Runner Configuration
apprunner_cpu           = "0.5 vCPU"
apprunner_memory        = "1 GB"
apprunner_min_size      = 2
apprunner_max_size      = 5
apprunner_max_concurrency = 100
apprunner_auto_deploy   = true

# Logging
log_retention_days = 7
