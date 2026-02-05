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
    max_attempts=30
    attempt=0
    until curl -s http://localhost:4566/_localstack/health | grep -q "running"; do
        attempt=$((attempt + 1))
        if [ $attempt -ge $max_attempts ]; then
            echo "⚠ LocalStack health check timeout, proceeding anyway..."
            break
        fi
        echo "  - LocalStack not ready yet, waiting... (attempt $attempt/$max_attempts)"
        sleep 2
    done
    echo "✓ LocalStack is available"
}

wait_for_localstack

echo ""
echo "===  Creating DynamoDB Tables ==="
echo ""

# ==========================================
# TABLE: Events (Event Sourcing)
# ==========================================
echo "Creating table: Events"

# Create Events table for Event Sourcing
aws --endpoint-url http://localhost:4566 dynamodb create-table \
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
# TABLE: EventAggregates (Event Aggregates - Current State)
# ==========================================
echo "Creating table: EventAggregates"

aws --endpoint-url http://localhost:4566 dynamodb create-table \
    --table-name EventAggregates \
    --attribute-definitions \
        AttributeName=eventId,AttributeType=S \
        AttributeName=status,AttributeType=S \
        AttributeName=eventDate,AttributeType=N \
    --key-schema \
        AttributeName=eventId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[{
            "IndexName": "StatusIndex",
            "KeySchema": [
                {"AttributeName": "status", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        },
        {
            "IndexName": "EventDateIndex",
            "KeySchema": [
                {"AttributeName": "eventDate", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        }]' \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'EventAggregates' created successfully"
else
    echo "⚠ Table 'EventAggregates' already exists"
fi

# ==========================================
# TABLE: TicketOrders (Ticket Orders)
# ==========================================
echo "Creating table: TicketOrders"

aws --endpoint-url http://localhost:4566 dynamodb create-table \
    --table-name TicketOrders \
    --attribute-definitions \
        AttributeName=orderId,AttributeType=S \
        AttributeName=customerId,AttributeType=S \
        AttributeName=createdAt,AttributeType=N \
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

aws --endpoint-url http://localhost:4566 dynamodb create-table \
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

aws --endpoint-url http://localhost:4566 dynamodb create-table \
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

# ==========================================
# TABLE: TicketStateTransitionAudit (Ticket State Transition Audit)
# ==========================================
echo "Creating table: TicketStateTransitionAudit"

aws --endpoint-url http://localhost:4566 dynamodb create-table \
    --table-name TicketStateTransitionAudit \
    --attribute-definitions \
        AttributeName=auditId,AttributeType=S \
        AttributeName=ticketId,AttributeType=S \
    --key-schema \
        AttributeName=auditId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[{
            "IndexName": "ticketId-index",
            "KeySchema": [
                {"AttributeName": "ticketId", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        }]' \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'TicketStateTransitionAudit' created successfully"
else
    echo "⚠ Table 'TicketStateTransitionAudit' already exists"
fi

# ==========================================
# TABLE: TicketItems (Individual Tickets)
# ==========================================
echo "Creating table: TicketItems"

aws --endpoint-url http://localhost:4566 dynamodb create-table \
    --table-name TicketItems \
    --attribute-definitions \
        AttributeName=ticketId,AttributeType=S \
        AttributeName=orderId,AttributeType=S \
        AttributeName=reservationId,AttributeType=S \
    --key-schema \
        AttributeName=ticketId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[{
            "IndexName": "OrderIndex",
            "KeySchema": [
                {"AttributeName": "orderId", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        },{
            "IndexName": "ReservationIndex",
            "KeySchema": [
                {"AttributeName": "reservationId", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        }]' \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'TicketItems' created successfully"
else
    echo "⚠ Table 'TicketItems' already exists"
fi

# ==========================================
# TABLE: SeatReservations (Seat Uniqueness)
# ==========================================
echo "Creating table: SeatReservations"

aws --endpoint-url http://localhost:4566 dynamodb create-table \
    --table-name SeatReservations \
    --attribute-definitions \
        AttributeName=seatKey,AttributeType=S \
    --key-schema \
        AttributeName=seatKey,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'SeatReservations' created successfully"
else
    echo "⚠ Table 'SeatReservations' already exists"
fi

# ==========================================
# TABLE: CustomerInfo (Customer Payment Information)
# ==========================================
echo "Creating table: CustomerInfo"

aws --endpoint-url http://localhost:4566 dynamodb create-table \
    --table-name CustomerInfo \
    --attribute-definitions \
        AttributeName=orderId,AttributeType=S \
        AttributeName=customerId,AttributeType=S \
    --key-schema \
        AttributeName=orderId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[{
            "IndexName": "CustomerIndex",
            "KeySchema": [
                {"AttributeName": "customerId", "KeyType": "HASH"}
            ],
            "Projection": {"ProjectionType": "ALL"}
        }]' \
    --tags \
        Key=Environment,Value=Development \
        Key=Service,Value=EventTicket \
    2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Table 'CustomerInfo' created successfully"
else
    echo "⚠ Table 'CustomerInfo' already exists"
fi

echo ""
echo "=== 📨 Creating SQS Queues ==="
echo ""

# Main queue for ticket orders
# Check if queue exists first
echo "Checking if queue 'ticket-order-queue' exists..."
QUEUE_EXISTS=$(aws --endpoint-url http://localhost:4566 sqs get-queue-url --queue-name ticket-order-queue --output text --query 'QueueUrl' 2>&1)

if echo "$QUEUE_EXISTS" | grep -q "NonExistentQueue\|does not exist"; then
    echo "Creating queue: ticket-order-queue"
    CREATE_RESULT=$(aws --endpoint-url http://localhost:4566 sqs create-queue \
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
        2>&1)
    
    if [ $? -eq 0 ]; then
        echo "✓ Queue 'ticket-order-queue' created successfully"
        echo "$CREATE_RESULT" | grep -q "QueueUrl" && echo "  Queue URL: $(echo "$CREATE_RESULT" | grep -oP '"QueueUrl":\s*"\K[^"]+')"
    else
        echo "⚠ Error creating queue 'ticket-order-queue':"
        echo "$CREATE_RESULT"
    fi
else
    echo "✓ Queue 'ticket-order-queue' already exists: $QUEUE_EXISTS"
fi

# Queue for payment processing
aws --endpoint-url http://localhost:4566 sqs create-queue \
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
aws --endpoint-url http://localhost:4566 sqs create-queue \
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
aws --endpoint-url http://localhost:4566 sqs create-queue \
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
aws --endpoint-url http://localhost:4566 sqs create-queue \
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
echo "===  Listing Created Resources ==="
echo ""

# List DynamoDB tables
echo "DynamoDB Tables:"
aws --endpoint-url http://localhost:4566 dynamodb list-tables --output table

# List SQS queues
echo ""
echo "SQS Queues:"
aws --endpoint-url http://localhost:4566 sqs list-queues --output table

echo ""
echo "===  LocalStack Initialization Completed Successfully ==="
echo ""
echo " Summary:"
echo "  - DynamoDB: 8 tables created (Events, TicketOrders, TicketInventory, TicketReservations, TicketStateTransitionAudit, TicketItems, SeatReservations, CustomerInfo)"
echo "  - SQS: 5 queues created (ticket-order, payment, notification, dlq, fifo)"
echo ""
echo "🔗 Access:"
echo "  - LocalStack Gateway: http://localhost:4566"
echo "  - Health Check: http://localhost:4566/_localstack/health"
echo "  - Dashboard: https://app.localstack.cloud (requires account)"
echo ""
echo "  Useful Commands:"
echo "  - aws --endpoint-url http://localhost:4566 dynamodb scan --table-name Events"
echo "  - aws --endpoint-url http://localhost:4566 dynamodb scan --table-name TicketOrders"
echo "  - aws --endpoint-url http://localhost:4566 dynamodb scan --table-name TicketInventory"
echo "  - aws --endpoint-url http://localhost:4566 sqs list-queues"
echo "  - aws --endpoint-url http://localhost:4566 sqs receive-message --queue-url <URL>"
