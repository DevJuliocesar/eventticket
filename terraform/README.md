# EventTicket - Infraestructura Terraform

Infraestructura AWS para el sistema EventTicket usando Terraform.

## 📋 Estructura

```
terraform/
├── main.tf                    # Configuración principal
├── variables.tf               # Variables
├── modules/
│   ├── networking/           # VPC, subnets, NAT
│   ├── security/             # Security Groups, IAM
│   ├── data/                 # DynamoDB, SQS, Redis
│   └── application/          # ECS, ALB, Auto Scaling
└── environments/
    ├── dev/terraform.tfvars
    ├── staging/terraform.tfvars
    └── prod/terraform.tfvars
```

## 🏗️ Arquitectura

```
Internet → ALB (público) → ECS Fargate (privado) → DynamoDB/SQS/Redis
```

**Componentes:**
- **Networking:** VPC con subnets públicas/privadas, NAT Gateway
- **Aplicación:** ECS Fargate con ALB y Auto Scaling
- **Datos:** DynamoDB (3 tablas), SQS (2 colas), ElastiCache Redis
- **Seguridad:** Security Groups, IAM roles, encryption

## 🚀 Uso Rápido

```bash
# 1. Configurar variables
cp terraform.tfvars.example terraform.tfvars
# Editar app_image con tu imagen Docker

# 2. Inicializar
terraform init

# 3. Planificar
terraform plan -var-file=environments/dev/terraform.tfvars

# 4. Aplicar
terraform apply -var-file=environments/dev/terraform.tfvars
```

## 📊 Recursos Creados

### DynamoDB (3 tablas)
- `Events` - Event Sourcing
- `TicketOrders` - Órdenes
- `TicketInventory` - Inventario

### SQS (2 colas)
- `ticket-order-queue` - Procesamiento de órdenes
- `ticket-dlq` - Dead Letter Queue

### ElastiCache
- Redis cluster (single node en dev, multi-AZ en prod)

### ECS
- Cluster Fargate
- Service con Auto Scaling
- Application Load Balancer

## 🔒 Seguridad

- Security Groups con reglas mínimas
- IAM roles separados (execution vs. runtime)
- Encryption at rest habilitada
- ECS tasks en subnets privadas

## 📈 Escalabilidad

- Auto Scaling basado en CPU/memoria
- DynamoDB PAY_PER_REQUEST (escalado automático)
- Multi-AZ para alta disponibilidad

## 🏢 Entornos

Cada entorno tiene su propia VPC y configuración:
- **Dev:** 1 instancia, single NAT Gateway
- **Staging:** 2 instancias, multi-AZ
- **Prod:** 3+ instancias, multi-AZ

## 💰 Costos Estimados

- **Dev:** ~$60-80/mes
- **Staging:** ~$120-150/mes
- **Prod:** ~$300-500/mes (varía según tráfico)

## 📚 Documentación

- `QUICK_START.md` - Guía de despliegue rápido
- `DECISIONES_DISENO.md` - Decisiones técnicas (simplificado)

## ✅ Checklist

- [ ] Configurar `app_image` en terraform.tfvars
- [ ] Verificar permisos IAM
- [ ] Revisar variables por entorno
- [ ] Ejecutar `terraform plan` antes de `apply`
