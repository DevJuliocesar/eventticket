# Quick Start Guide

## Prerequisites

```bash
terraform version  # >= 1.5.0
aws sts get-caller-identity  # AWS CLI configured
```

## Deployment

```bash
# 1. Configure variables
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit variables according to your environment

# 2. Initialize
terraform init

# 3. Plan
terraform plan -var-file=environments/dev/terraform.tfvars

# 4. Apply (takes ~10 minutes)
terraform apply -var-file=environments/dev/terraform.tfvars
```

## Verify

```bash
# Get App Runner service URL
terraform output apprunner_service_url

# Test health check
curl https://$(terraform output -raw apprunner_service_url | cut -d'/' -f3)/actuator/health
```

## Prepare Docker Image

```bash
# 1. Get ECR repository URL (created by Terraform)
ECR_URL=$(terraform output -raw ecr_repository_url)

# 2. Authenticate
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $(echo $ECR_URL | cut -d'/' -f1)

# 3. Build and push
cd ..
docker build -t eventticket:latest .
docker tag eventticket:latest ${ECR_URL}:latest
docker push ${ECR_URL}:latest
```

**Note:** If `apprunner_auto_deploy = true`, App Runner will automatically detect the new push and deploy.

## Troubleshooting

**Error: Image not found**
- Verify image exists in ECR
- Verify ECR repository URL in terraform outputs

**Error: Health check failed**
- Verify `/actuator/health` is available
- Check CloudWatch logs: `/aws/apprunner/{environment}/{app_name}`

**Error: Cannot connect to Redis**
- Verify Security Groups allow VPC traffic
- Verify Redis is in private subnets

## Destroy

```bash
terraform destroy -var-file=environments/dev/terraform.tfvars
```
