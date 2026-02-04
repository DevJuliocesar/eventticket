# Guía de Migración: Fargate → App Runner

Esta guía explica cómo migrar de ECS Fargate a AWS App Runner usando Terraform.

## 📋 Cambios Realizados

### 1. Nuevo Módulo App Runner
- ✅ Creado `modules/apprunner/` con:
  - `main.tf` - Recursos de App Runner, ECR, IAM
  - `variables.tf` - Variables del módulo
  - `outputs.tf` - Outputs del módulo

### 2. Actualización de `main.tf`
- ✅ Módulo `application` comentado (mantener para rollback)
- ✅ Nuevo módulo `apprunner` activo
- ✅ Outputs actualizados para App Runner

### 3. Actualización de Variables
- ✅ Nuevas variables `apprunner_*` agregadas
- ✅ Variables `ecs_*` comentadas (mantener para referencia)

## 🚀 Pasos para Aplicar la Migración

### Paso 1: Revisar Configuración

```bash
cd terraform

# Revisar las nuevas variables
cat terraform.tfvars.example

# Actualizar tu archivo de variables si es necesario
# environments/dev/terraform.tfvars
```

### Paso 2: Inicializar Terraform

```bash
# Inicializar (descargará el provider de AWS si es necesario)
terraform init
```

### Paso 3: Planificar Cambios

```bash
# Ver qué recursos se crearán/eliminarán
terraform plan -var-file=environments/dev/terraform.tfvars
```

**Recursos que se crearán:**
- ✅ ECR Repository
- ✅ App Runner Service
- ✅ App Runner Auto Scaling Configuration
- ✅ IAM Role para App Runner
- ✅ CloudWatch Log Group

**Recursos que se eliminarán:**
- ❌ ECS Cluster
- ❌ ECS Service
- ❌ Application Load Balancer
- ❌ Target Group
- ❌ Task Definition

**Recursos que se mantienen:**
- ✅ VPC, Subnets, NAT Gateway
- ✅ DynamoDB Tables
- ✅ SQS Queues
- ✅ ElastiCache Redis
- ✅ Security Groups (algunos ya no se usan)

### Paso 4: Aplicar Cambios

```bash
# Aplicar la migración
terraform apply -var-file=environments/dev/terraform.tfvars
```

**⏱️ Tiempo estimado: 5-10 minutos**

### Paso 5: Obtener URL del Servicio

```bash
# Obtener la URL del servicio App Runner
terraform output apprunner_service_url
```

La URL será algo como: `https://xxxxx.us-east-1.awsapprunner.com`

### Paso 6: Build y Push de Imagen Docker

```bash
# 1. Obtener URL del repositorio ECR
terraform output ecr_repository_url

# 2. Login a ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $(terraform output -raw ecr_repository_url | cut -d'/' -f1)

# 3. Build de la imagen
cd ..
docker build -t eventticket .

# 4. Tag y push
ECR_URL=$(terraform output -raw ecr_repository_url)
docker tag eventticket:latest ${ECR_URL}:latest
docker push ${ECR_URL}:latest
```

**Nota:** App Runner detectará automáticamente el nuevo push si `auto_deploy = true`

### Paso 7: Verificar Despliegue

```bash
# Verificar que el servicio esté corriendo
aws apprunner describe-service \
  --service-arn $(terraform output -raw apprunner_service_arn) \
  --region us-east-1

# Probar el endpoint
curl $(terraform output -raw apprunner_service_url)/actuator/health
```

## 🔄 Rollback (Si es Necesario)

Si necesitas volver a Fargate:

### 1. Revertir Cambios en `main.tf`

```bash
# Descomentar módulo application
# Comentar módulo apprunner
```

### 2. Aplicar Rollback

```bash
terraform plan -var-file=environments/dev/terraform.tfvars
terraform apply -var-file=environments/dev/terraform.tfvars
```

## 📊 Comparación: Antes vs Después

| Aspecto | Fargate (Antes) | App Runner (Después) |
|---------|----------------|---------------------|
| **Recursos Terraform** | ~200+ líneas | ~50 líneas |
| **Recursos AWS** | 10+ recursos | 5 recursos |
| **Costo/mes** | ~$73 | ~$10-20 |
| **Setup tiempo** | Días | 30 minutos |
| **Auto-scaling** | Configurado manualmente | Automático |
| **HTTPS** | Requiere ALB + ACM | Incluido |
| **Load Balancer** | ALB separado | Incluido |
| **Mantenimiento** | Alto | Bajo |

## ✅ Ventajas de la Migración

1. **75% menos código Terraform**
2. **70% más barato** (~$50-60/mes de ahorro)
3. **Misma funcionalidad** (auto-scaling, HTTPS, integración AWS)
4. **Más simple de mantener** (menos recursos)
5. **Deploy más rápido** (solo push a ECR)

## ⚠️ Consideraciones

### Lo que NO cambia:
- ✅ DynamoDB, SQS, Redis siguen igual
- ✅ Variables de entorno funcionan igual
- ✅ Health checks igual (`/actuator/health`)
- ✅ Logs en CloudWatch

### Lo que cambia:
- ❌ Ya no hay ALB (App Runner tiene su propio load balancer)
- ❌ Ya no hay ECS Cluster/Service
- ❌ Ya no necesitas Security Groups para ECS
- ❌ La URL es diferente (App Runner URL vs ALB DNS)

### Limitaciones de App Runner:
- ⚠️ No puedes configurar VPC complejas (pero no las necesitas)
- ⚠️ No puedes usar Security Groups personalizados (usa IAM)
- ⚠️ Menos control sobre networking (suficiente para la mayoría de casos)

## 🔧 Configuración de Variables de Entorno

App Runner automáticamente configura:
- `SPRING_PROFILES_ACTIVE` = environment
- `AWS_REGION` = aws_region
- `SPRING_DATA_REDIS_HOST` = redis_endpoint

Para agregar más variables, edita `modules/apprunner/main.tf`:
```hcl
runtime_environment_variables = merge(
  { ... },
  var.extra_environment_variables  # Agregar aquí
)
```

## 📝 Próximos Pasos

1. ✅ Aplicar migración en dev
2. ✅ Probar la aplicación
3. ✅ Verificar logs en CloudWatch
4. ✅ Aplicar en staging
5. ✅ Aplicar en producción

## 🆘 Troubleshooting

### Error: "Image not found"
- Verifica que hayas hecho push de la imagen a ECR
- Verifica que el tag sea `:latest`

### Error: "Health check failing"
- Verifica que `/actuator/health` esté disponible
- Revisa logs en CloudWatch: `/aws/apprunner/{environment}/{app_name}`

### Error: "Permission denied"
- Verifica que el IAM role tenga permisos para ECR, DynamoDB, SQS

## 📚 Referencias

- [AWS App Runner Documentation](https://docs.aws.amazon.com/apprunner/)
- [Terraform AWS App Runner Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/apprunner_service)
