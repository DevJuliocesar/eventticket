# EventTicket - Terraform Infrastructure

AWS infrastructure for EventTicket system using Terraform with AWS App Runner.

## Structure

```
terraform/
├── main.tf                    # Main configuration
├── variables.tf               # Variables
├── modules/
│   ├── networking/           # VPC, subnets, NAT
│   ├── security/             # Security Groups (Redis)
│   ├── data/                 # DynamoDB, SQS, Redis
│   └── apprunner/            # App Runner, ECR, Auto Scaling
└── environments/
    ├── dev/terraform.tfvars
    ├── staging/terraform.tfvars
    └── prod/terraform.tfvars
```

## Architecture

```
Internet → App Runner (automatic HTTPS) → DynamoDB/SQS/Redis
```

**Components:**
- **Networking:** VPC with public/private subnets, NAT Gateway
- **Application:** AWS App Runner with auto-scaling and HTTPS included
- **Data:** DynamoDB (3 tables), SQS (2 queues), ElastiCache Redis
- **Security:** Security Groups (Redis), IAM roles (App Runner)

## Quick Start

```bash
# 1. Configure variables
cp terraform.tfvars.example terraform.tfvars
# Edit variables according to your environment

# 2. Initialize
terraform init

# 3. Plan
terraform plan -var-file=environments/dev/terraform.tfvars

# 4. Apply
terraform apply -var-file=environments/dev/terraform.tfvars

# 5. Get service URL
terraform output apprunner_service_url

# 6. Get ECR repository URL for Docker image push
terraform output ecr_repository_url
```

## Created Resources

### DynamoDB (3 tables)
- `Events` - Event Sourcing
- `TicketOrders` - Orders
- `TicketInventory` - Inventory

### SQS (2 queues)
- `ticket-order-queue` - Order processing
- `ticket-dlq` - Dead Letter Queue

### ElastiCache
- Redis cluster (single node in dev, multi-AZ in prod)

### App Runner
- App Runner service with automatic auto-scaling
- ECR Repository for Docker images
- HTTPS included (no additional configuration)
- Auto-deploy when image is updated

## Security

- Security Groups with minimal rules (Redis only)
- IAM roles for App Runner (access and instance)
- Encryption at rest enabled
- Automatic HTTPS in App Runner

## Scalability

- Auto Scaling based on CPU/memory
- DynamoDB PAY_PER_REQUEST (automatic scaling)
- Multi-AZ for high availability

## Environments

Each environment has its own VPC and configuration:
- **Dev:** 1 minimum instance, single NAT Gateway
- **Staging:** 1-5 instances, multi-AZ
- **Prod:** 1-20 instances, multi-AZ

## Estimated Costs

- **Dev:** ~$30-40/month (App Runner + NAT Gateway + Redis)
- **Staging:** ~$50-70/month
- **Prod:** ~$100-200/month (varies by traffic)

**Savings vs Fargate:** ~70% cheaper

## Documentation

- `APP_RUNNER_MIGRATION.md` - Migration guide and usage
- `QUICK_START.md` - Quick deployment guide (if exists)

## Build and Push Docker Image

```bash
# 1. Get ECR URL
ECR_URL=$(terraform output -raw ecr_repository_url)

# 2. Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $(echo $ECR_URL | cut -d'/' -f1)

# 3. Build
docker build -t eventticket .

# 4. Tag and push
docker tag eventticket:latest ${ECR_URL}:latest
docker push ${ECR_URL}:latest
```

**Note:** If `apprunner_auto_deploy = true`, App Runner will automatically detect the new push and deploy.

## Checklist

- [ ] Configure variables in `environments/dev/terraform.tfvars`
- [ ] Verify IAM permissions
- [ ] Review variables per environment
- [ ] Run `terraform plan` before `apply`
- [ ] Build and push Docker image to ECR
- [ ] Verify service is running