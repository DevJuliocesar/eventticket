# Guía de Inicio Rápido

## Prerrequisitos

```bash
terraform version  # >= 1.5.0
aws sts get-caller-identity  # AWS CLI configurado
```

## Despliegue

```bash
# 1. Configurar variables
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Editar app_image con tu imagen Docker en ECR

# 2. Inicializar
terraform init

# 3. Planificar
terraform plan -var-file=environments/dev/terraform.tfvars

# 4. Aplicar (toma ~15 minutos)
terraform apply -var-file=environments/dev/terraform.tfvars
```

## Verificar

```bash
# Obtener URL del ALB
terraform output alb_dns_name

# Probar health check
curl http://$(terraform output -raw alb_dns_name)/actuator/health
```

## Preparar Imagen Docker

```bash
# 1. Crear repositorio ECR
aws ecr create-repository --repository-name eventticket

# 2. Autenticarse
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# 3. Construir y push
docker build -t eventticket:latest ..
docker tag eventticket:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/eventticket:latest
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/eventticket:latest
```

## Troubleshooting

**Error: Image not found**
- Verificar que la imagen existe en ECR
- Verificar URI en terraform.tfvars

**Error: Health check failed**
- Verificar que `/actuator/health` está disponible
- Verificar Security Groups

**Error: Cannot connect to Redis**
- Verificar Security Groups
- Verificar que Redis está en subnets privadas

## Destruir

```bash
terraform destroy -var-file=environments/dev/terraform.tfvars
```
