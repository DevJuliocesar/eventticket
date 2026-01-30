#!/bin/bash
# LocalStack Initialization Script (DynamoDB + SQS)
# This script runs automatically when LocalStack is ready

echo "=== Initializing LocalStack (DynamoDB + SQS) ==="
echo "Date: $(date)"
echo ""

# Configure AWS CLI for LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566

# Wait for LocalStack to be ready
wait_for_localstack() {
    echo "Waiting for LocalStack to be available..."
    until curl -s http://localhost:4566/_localstack/health | grep -q "running"; do
        echo "  - LocalStack not ready yet, waiting..."
        sleep 2
    done
    echo "✓ LocalStack is available"
}

wait_for_localstack

echo ""
echo "=== 📊 Creating DynamoDB Tables ==="
echo ""

# ==========================================
# TABLE: Events (Event Sourcing)
# ==========================================
echo "Creating table: Events"

# Create Events table for Event Sourcing
awslocal dynamodb create-table \
    --table-name Events \
    --attribute-definitions \
        AttributeName=aggregateId,AttributeType=S \
        AttributeName=version,AttributeType=N \
        AttributeName=eventType,AttributeType=S \
        AttributeName=createdAt,AttributeType=S \
    --key-schema \
        AttributeName=aggregateId,KeyType=HASH \
        AttributeName=version,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[{
            "IndexName": "EventTypeIndex",
            "KeySchema": [
                {"AttributeName": "eventType", "KeyType": "HASH"},
                {"AttributeName": "createdAt", "KeyType": "RANGE"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        }]' \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'Events' created successfully"
else
    echo "⚠ Table 'Events' already exists or there was an error"
fi

# ==========================================
# TABLE: TicketOrders (Ticket Orders)
# ==========================================
echo "Creating table: TicketOrders"

awslocal dynamodb create-table \
    --table-name TicketOrders \
    --attribute-definitions \
        AttributeName=orderId,AttributeType=S \
        AttributeName=customerId,AttributeType=S \
        AttributeName=createdAt,AttributeType=S \
        AttributeName=status,AttributeType=S \
    --key-schema \
        AttributeName=orderId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[{
            "IndexName": "CustomerIndex",
            "KeySchema": [
                {"AttributeName": "customerId", "KeyType": "HASH"},
                {"AttributeName": "createdAt", "KeyType": "RANGE"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        },
        {
            "IndexName": "StatusIndex",
            "KeySchema": [
                {"AttributeName": "status", "KeyType": "HASH"},
                {"AttributeName": "createdAt", "KeyType": "RANGE"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        }]' \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'TicketOrders' created successfully"
else
    echo "⚠ Table 'TicketOrders' already exists"
fi

# ==========================================
# TABLE: TicketInventory (Ticket Inventory)
# ==========================================
echo "Creating table: TicketInventory"

awslocal dynamodb create-table \
    --table-name TicketInventory \
    --attribute-definitions \
        AttributeName=eventId,AttributeType=S \
        AttributeName=ticketType,AttributeType=S \
    --key-schema \
        AttributeName=eventId,KeyType=HASH \
        AttributeName=ticketType,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'TicketInventory' created successfully"
else
    echo "⚠ Table 'TicketInventory' already exists"
fi

# ==========================================
# TABLE: TicketReservations (Ticket Reservations)
# ==========================================
echo "Creating table: TicketReservations"

awslocal dynamodb create-table \
    --table-name TicketReservations \
    --attribute-definitions \
        AttributeName=reservationId,AttributeType=S \
        AttributeName=orderId,AttributeType=S \
        AttributeName=expiresAt,AttributeType=N \
    --key-schema \
        AttributeName=reservationId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[{
            "IndexName": "OrderIndex",
            "KeySchema": [
                {"AttributeName": "orderId", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        },
        {
            "IndexName": "ExpirationIndex",
            "KeySchema": [
                {"AttributeName": "expiresAt", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        }]' \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'TicketReservations' created successfully"
else
    echo "⚠ Table 'TicketReservations' already exists"
fi

echo ""
echo "=== 📨 Creating SQS Queues ==="
echo ""

# Main queue for ticket orders
awslocal sqs create-queue \
    --queue-name ticket-order-queue \
    --attributes '{
        "DelaySeconds": "0",
        "MessageRetentionPeriod": "345600",
        "ReceiveMessageWaitTimeSeconds": "10",
        "VisibilityTimeout": "30"
    }' \
    --tags \
        Environment=Development \
        Service=EventTicket \
    2>/dev/null

echo "✓ Queue 'ticket-order-queue' created"

# Queue for payment processing
awslocal sqs create-queue \
    --queue-name ticket-payment-queue \
    --attributes '{
        "DelaySeconds": "0",
        "MessageRetentionPeriod": "345600",
        "ReceiveMessageWaitTimeSeconds": "10",
        "VisibilityTimeout": "60"
    }' \
    --tags \
        Environment=Development \
        Service=EventTicket \
    2>/dev/null

echo "✓ Queue 'ticket-payment-queue' created"

# Queue for notifications
awslocal sqs create-queue \
    --queue-name ticket-notification-queue \
    --attributes '{
        "DelaySeconds": "0",
        "MessageRetentionPeriod": "345600",
        "ReceiveMessageWaitTimeSeconds": "10",
        "VisibilityTimeout": "30"
    }' \
    --tags \
        Environment=Development \
        Service=EventTicket \
    2>/dev/null

echo "✓ Queue 'ticket-notification-queue' created"

# Dead Letter Queue (DLQ) for failed messages
awslocal sqs create-queue \
    --queue-name ticket-dlq \
    --attributes '{
        "MessageRetentionPeriod": "1209600"
    }' \
    --tags \
        Environment=Development \
        Service=EventTicket \
    2>/dev/null

echo "✓ DLQ 'ticket-dlq' created"

# FIFO queue for ordered processing
awslocal sqs create-queue \
    --queue-name ticket-order-fifo.fifo \
    --attributes '{
        "FifoQueue": "true",
        "ContentBasedDeduplication": "true",
        "MessageRetentionPeriod": "345600"
    }' \
    --tags \
        Environment=Development \
        Service=EventTicket \
    2>/dev/null

echo "✓ FIFO queue 'ticket-order-fifo.fifo' created"

echo ""
echo "=== 📥 Inserting Sample Data ==="
echo ""

# ==========================================
# Sample Events
# ==========================================
echo "Inserting sample events..."

# Event 1: TicketOrderCreated
awslocal dynamodb put-item \
    --table-name Events \
    --item '{
        "aggregateId": {"S": "ticket-order-001"},
        "version": {"N": "1"},
        "aggregateType": {"S": "TicketOrder"},
        "eventType": {"S": "TicketOrderCreated"},
        "eventData": {"S": "{\"orderId\":\"ticket-order-001\",\"customerId\":\"customer-001\",\"eventId\":\"concert-001\",\"ticketCount\":2,\"totalAmount\":150000.0,\"currency\":\"COP\"}"},
        "createdAt": {"S": "2026-01-30T12:00:00Z"},
        "metadata": {"S": "{\"correlationId\":\"corr-001\",\"userId\":\"system\",\"source\":\"web\"}"}
    }' \
    2>/dev/null

# Evento 2: TicketReserved
awslocal dynamodb put-item \
    --table-name Events \
    --item '{
        "aggregateId": {"S": "ticket-order-001"},
        "version": {"N": "2"},
        "aggregateType": {"S": "TicketOrder"},
        "eventType": {"S": "TicketReserved"},
        "eventData": {"S": "{\"orderId\":\"ticket-order-001\",\"eventId\":\"concert-001\",\"ticketIds\":[\"ticket-001\",\"ticket-002\"],\"reservedUntil\":\"2026-01-30T12:15:00Z\"}"},
        "createdAt": {"S": "2026-01-30T12:00:05Z"},
        "metadata": {"S": "{\"correlationId\":\"corr-001\",\"userId\":\"system\"}"}
    }' \
    2>/dev/null

# Evento 3: PaymentProcessed
awslocal dynamodb put-item \
    --table-name Events \
    --item '{
        "aggregateId": {"S": "ticket-order-001"},
        "version": {"N": "3"},
        "aggregateType": {"S": "TicketOrder"},
        "eventType": {"S": "PaymentProcessed"},
        "eventData": {"S": "{\"orderId\":\"ticket-order-001\",\"paymentId\":\"pay-001\",\"amount\":150000.0,\"status\":\"COMPLETED\"}"},
        "createdAt": {"S": "2026-01-30T12:00:10Z"},
        "metadata": {"S": "{\"correlationId\":\"corr-001\",\"userId\":\"system\"}"}
    }' \
    2>/dev/null

echo "✓ 3 sample events inserted into DynamoDB"

# ==========================================
# Sample Ticket Orders
# ==========================================
echo "Inserting sample ticket orders..."

# Order 1
awslocal dynamodb put-item \
    --table-name TicketOrders \
    --item '{
        "orderId": {"S": "order-001"},
        "customerId": {"S": "customer-001"},
        "orderNumber": {"S": "ORD-2026-00001"},
        "eventId": {"S": "concert-001"},
        "eventName": {"S": "Summer Music Festival 2026"},
        "status": {"S": "CONFIRMED"},
        "tickets": {"L": [
            {"M": {
                "ticketId": {"S": "ticket-001"},
                "ticketType": {"S": "VIP"},
                "seatNumber": {"S": "A-15"},
                "price": {"N": "150000"}
            }},
            {"M": {
                "ticketId": {"S": "ticket-002"},
                "ticketType": {"S": "VIP"},
                "seatNumber": {"S": "A-16"},
                "price": {"N": "150000"}
            }}
        ]},
        "totalAmount": {"N": "300000"},
        "currency": {"S": "COP"},
        "createdAt": {"S": "2026-01-30T12:00:00Z"},
        "updatedAt": {"S": "2026-01-30T12:05:00Z"}
    }' \
    2>/dev/null

echo "✓ Sample order 'order-001' inserted"

# ==========================================
# Sample Inventory
# ==========================================
echo "Inserting sample inventory..."

# Event 1: Concert
awslocal dynamodb put-item \
    --table-name TicketInventory \
    --item '{
        "eventId": {"S": "concert-001"},
        "ticketType": {"S": "VIP"},
        "eventName": {"S": "Summer Music Festival 2026"},
        "totalQuantity": {"N": "100"},
        "availableQuantity": {"N": "75"},
        "reservedQuantity": {"N": "25"},
        "price": {"N": "150000"},
        "currency": {"S": "COP"},
        "version": {"N": "1"}
    }' \
    2>/dev/null

awslocal dynamodb put-item \
    --table-name TicketInventory \
    --item '{
        "eventId": {"S": "concert-001"},
        "ticketType": {"S": "GENERAL"},
        "eventName": {"S": "Summer Music Festival 2026"},
        "totalQuantity": {"N": "500"},
        "availableQuantity": {"N": "450"},
        "reservedQuantity": {"N": "50"},
        "price": {"N": "75000"},
        "currency": {"S": "COP"},
        "version": {"N": "1"}
    }' \
    2>/dev/null

echo "✓ Sample inventory items inserted"

# ==========================================
# Sample Reservation
# ==========================================
echo "Inserting sample reservation..."

awslocal dynamodb put-item \
    --table-name TicketReservations \
    --item '{
        "reservationId": {"S": "res-001"},
        "orderId": {"S": "order-002"},
        "eventId": {"S": "concert-001"},
        "ticketType": {"S": "VIP"},
        "quantity": {"N": "2"},
        "status": {"S": "ACTIVE"},
        "expiresAt": {"N": "1738248000"},
        "createdAt": {"S": "2026-01-30T12:00:00Z"}
    }' \
    2>/dev/null

echo "✓ Sample reservation inserted"

echo ""
echo "=== 📤 Sending Sample Messages to SQS ==="
echo ""

# Get queue URLs
TICKET_ORDER_QUEUE_URL=$(awslocal sqs get-queue-url --queue-name ticket-order-queue --output text --query 'QueueUrl')
TICKET_PAYMENT_QUEUE_URL=$(awslocal sqs get-queue-url --queue-name ticket-payment-queue --output text --query 'QueueUrl')

# Send test message to ticket-order-queue
awslocal sqs send-message \
    --queue-url "$TICKET_ORDER_QUEUE_URL" \
    --message-body '{
        "orderId": "order-002",
        "customerId": "customer-002",
        "eventId": "concert-002",
        "ticketCount": 4,
        "totalAmount": 300000.0,
        "timestamp": "2026-01-30T12:05:00Z"
    }' \
    --message-attributes '{
        "eventType": {"DataType": "String", "StringValue": "TicketOrderCreated"},
        "priority": {"DataType": "Number", "StringValue": "1"}
    }' \
    2>/dev/null

echo "✓ Message sent to 'ticket-order-queue'"

# Send test message to ticket-payment-queue
awslocal sqs send-message \
    --queue-url "$TICKET_PAYMENT_QUEUE_URL" \
    --message-body '{
        "orderId": "order-002",
        "paymentMethod": "credit_card",
        "amount": 300000.0,
        "timestamp": "2026-01-30T12:05:30Z"
    }' \
    2>/dev/null

echo "✓ Message sent to 'ticket-payment-queue'"

echo ""
echo "=== 📋 Listing Created Resources ==="
echo ""

# List DynamoDB tables
echo "DynamoDB Tables:"
awslocal dynamodb list-tables --output table

# List SQS queues
echo ""
echo "SQS Queues:"
awslocal sqs list-queues --output table

echo ""
echo "=== ✅ LocalStack Initialization Completed Successfully ==="
echo ""
echo "📊 Summary:"
echo "  - DynamoDB: 4 tables created (Events, TicketOrders, TicketInventory, TicketReservations)"
echo "  - SQS: 5 queues created (ticket-order, payment, notification, dlq, fifo)"
echo "  - Sample events: 3 inserted into Events table"
echo "  - Sample orders: 1 inserted into TicketOrders table"
echo "  - Sample inventory: 2 items inserted"
echo "  - Sample messages: 2 sent to SQS queues"
echo ""
echo "🔗 Access:"
echo "  - LocalStack Gateway: http://localhost:4566"
echo "  - Health Check: http://localhost:4566/_localstack/health"
echo "  - Dashboard: https://app.localstack.cloud (requires account)"
echo ""
echo "🛠️  Useful Commands:"
echo "  - awslocal dynamodb scan --table-name Events"
echo "  - awslocal dynamodb scan --table-name TicketOrders"
echo "  - awslocal dynamodb scan --table-name TicketInventory"
echo "  - awslocal sqs list-queues"
echo "  - awslocal sqs receive-message --queue-url <URL>"
