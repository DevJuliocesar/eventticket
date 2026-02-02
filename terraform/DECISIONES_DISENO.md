# Decisiones de Diseño - Infraestructura EventTicket

Documento simplificado con las decisiones principales de diseño.

## 🏗️ Networking

### VPC con Subnets Públicas/Privadas

**Decisión:** VPC con subnets públicas para ALB y privadas para ECS.

**Justificación:**
- Seguridad: ECS tasks sin IPs públicas
- Mejores prácticas AWS
- Aislamiento de capas

### NAT Gateway

**Decisión:** 1 NAT compartido en dev, 1 por AZ en prod.

**Justificación:**
- Dev: Optimización de costos (~$32/mes)
- Prod: Alta disponibilidad (~$64/mes)

## 🔒 Seguridad

### Security Groups

**Decisión:** Reglas mínimas necesarias.

**Implementación:**
- ALB: HTTP/HTTPS desde internet
- ECS: Puerto 8080 solo desde ALB
- Redis: Puerto 6379 solo desde ECS

### IAM Roles Separados

**Decisión:** Dos roles: `ecs_task_execution` (pull images) y `ecs_task` (runtime).

**Justificación:**
- Principio de menor privilegio
- Separación de responsabilidades

## 📈 Escalabilidad

### ECS Fargate

**Decisión:** Fargate en lugar de EC2.

**Justificación:**
- Sin gestión de servidores
- Auto-scaling nativo
- Pago por uso

### DynamoDB PAY_PER_REQUEST

**Decisión:** PAY_PER_REQUEST en lugar de provisioned.

**Justificación:**
- Escalado automático
- Ideal para Event Sourcing
- Sin previsión de capacidad

### Auto Scaling

**Decisión:** Target Tracking basado en CPU (70%) y memoria (80%).

**Configuración:**
- Dev: 1-3 instancias
- Staging: 2-5 instancias
- Prod: 3-20 instancias

## 🏢 Aislamiento de Entornos

### VPCs Separadas

**Decisión:** Una VPC por entorno.

**Justificación:**
- Aislamiento completo
- Seguridad
- Gestión independiente

**CIDR Blocks:**
- Dev: `10.0.0.0/16`
- Staging: `10.1.0.0/16`
- Prod: `10.2.0.0/16`

### Naming Convention

**Formato:** `{environment}-{app_name}-{resource_type}`

**Ejemplo:** `dev-eventticket-cluster`

## 💰 Optimizaciones de Costo

1. **Dev:** Single NAT Gateway, menor capacidad ECS
2. **Log Retention:** 3 días en dev, 30 en prod
3. **Redis:** Single node en dev, multi-AZ en prod

## 📊 Recursos Simplificados

### DynamoDB
- Solo 3 tablas esenciales (Events, TicketOrders, TicketInventory)
- Sin GSI complejos (solo los necesarios)

### SQS
- Solo 2 colas (order queue + DLQ)
- Sin colas adicionales (payment, notification)

### ElastiCache
- Configuración simplificada
- Single node en dev, multi-AZ en prod

## 🔄 Consideraciones Futuras

- HTTPS/SSL con ACM
- WAF delante del ALB
- VPC Endpoints para DynamoDB/SQS
- Secrets Manager para credenciales
