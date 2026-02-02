package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.CustomerInfo;
import com.eventticket.domain.repository.CustomerInfoRepository;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB implementation of CustomerInfoRepository.
 * Uses AWS SDK v2 Async for reactive operations.
 */
@Repository
public class DynamoDBCustomerInfoRepository implements CustomerInfoRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDBCustomerInfoRepository.class);
    private static final String TABLE_NAME = "CustomerInfo";

    private final DynamoDbAsyncClient dynamoDbClient;

    public DynamoDBCustomerInfoRepository(DynamoDbAsyncClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Mono<CustomerInfo> save(CustomerInfo customerInfo) {
        log.debug("Saving customer info to DynamoDB: orderId={}, customerId={}", 
                customerInfo.getOrderId().value(), customerInfo.getCustomerId().value());
        
        Map<String, AttributeValue> item = toDynamoDBItem(customerInfo);
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        return Mono.fromFuture(dynamoDbClient.putItem(request))
                .then(Mono.just(customerInfo))
                .doOnSuccess(c -> log.debug("Customer info saved successfully: orderId={}", 
                        c.getOrderId().value()))
                .doOnError(error -> log.error("Error saving customer info to DynamoDB", error));
    }

    @Override
    public Mono<CustomerInfo> findByOrderId(OrderId orderId) {
        log.debug("Finding customer info by orderId: {}", orderId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("orderId", AttributeValue.builder().s(orderId.value()).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.getItem(request))
                .flatMap(response -> {
                    if (response.hasItem()) {
                        CustomerInfo customerInfo = fromDynamoDBItem(response.item());
                        return Mono.just(customerInfo);
                    }
                    return Mono.empty();
                })
                .doOnSuccess(c -> {
                    if (c != null) {
                        log.debug("Customer info found: orderId={}", orderId.value());
                    }
                })
                .doOnError(error -> log.error("Error finding customer info in DynamoDB", error));
    }

    @Override
    public Flux<CustomerInfo> findByCustomerId(CustomerId customerId) {
        log.debug("Finding customer info by customerId: {}", customerId.value());
        
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":customerId", AttributeValue.builder().s(customerId.value()).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("customerId = :customerId")
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        return Flux.from(dynamoDbClient.scanPaginator(request))
                .flatMap(response -> Flux.fromIterable(response.items()))
                .map(this::fromDynamoDBItem)
                .doOnError(error -> log.error("Error finding customer info by customerId in DynamoDB", error));
    }

    @Override
    public Mono<Void> deleteByOrderId(OrderId orderId) {
        log.debug("Deleting customer info: orderId={}", orderId.value());
        
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("orderId", AttributeValue.builder().s(orderId.value()).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        return Mono.fromFuture(dynamoDbClient.deleteItem(request))
                .then()
                .doOnSuccess(v -> log.debug("Customer info deleted: orderId={}", orderId.value()))
                .doOnError(error -> log.error("Error deleting customer info from DynamoDB", error));
    }

    /**
     * Converts CustomerInfo domain object to DynamoDB item.
     */
    private Map<String, AttributeValue> toDynamoDBItem(CustomerInfo customerInfo) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("orderId", AttributeValue.builder().s(customerInfo.getOrderId().value()).build());
        item.put("customerId", AttributeValue.builder().s(customerInfo.getCustomerId().value()).build());
        item.put("customerName", AttributeValue.builder().s(customerInfo.getCustomerName()).build());
        item.put("email", AttributeValue.builder().s(customerInfo.getEmail()).build());
        item.put("phoneNumber", AttributeValue.builder().s(customerInfo.getPhoneNumber()).build());
        
        if (customerInfo.getAddress() != null) {
            item.put("address", AttributeValue.builder().s(customerInfo.getAddress()).build());
        }
        if (customerInfo.getCity() != null) {
            item.put("city", AttributeValue.builder().s(customerInfo.getCity()).build());
        }
        if (customerInfo.getCountry() != null) {
            item.put("country", AttributeValue.builder().s(customerInfo.getCountry()).build());
        }
        if (customerInfo.getPaymentMethod() != null) {
            item.put("paymentMethod", AttributeValue.builder().s(customerInfo.getPaymentMethod()).build());
        }
        
        item.put("createdAt", AttributeValue.builder().n(String.valueOf(customerInfo.getCreatedAt().getEpochSecond())).build());
        item.put("updatedAt", AttributeValue.builder().n(String.valueOf(customerInfo.getUpdatedAt().getEpochSecond())).build());
        
        return item;
    }

    /**
     * Converts DynamoDB item to CustomerInfo domain object.
     * Uses reflection to access private constructor.
     */
    private CustomerInfo fromDynamoDBItem(Map<String, AttributeValue> item) {
        try {
            OrderId orderId = OrderId.of(item.get("orderId").s());
            CustomerId customerId = CustomerId.of(item.get("customerId").s());
            String customerName = item.get("customerName").s();
            String email = item.get("email").s();
            String phoneNumber = item.get("phoneNumber").s();
            String address = item.containsKey("address") && item.get("address") != null 
                    ? item.get("address").s() 
                    : null;
            String city = item.containsKey("city") && item.get("city") != null 
                    ? item.get("city").s() 
                    : null;
            String country = item.containsKey("country") && item.get("country") != null 
                    ? item.get("country").s() 
                    : null;
            String paymentMethod = item.containsKey("paymentMethod") && item.get("paymentMethod") != null 
                    ? item.get("paymentMethod").s() 
                    : null;
            
            Instant createdAt = Instant.ofEpochSecond(Long.parseLong(item.get("createdAt").n()));
            Instant updatedAt = Instant.ofEpochSecond(Long.parseLong(item.get("updatedAt").n()));

            // Use reflection to access private constructor
            Constructor<CustomerInfo> constructor = CustomerInfo.class.getDeclaredConstructor(
                    CustomerId.class, OrderId.class, String.class, String.class, String.class,
                    String.class, String.class, String.class, String.class,
                    Instant.class, Instant.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    customerId, orderId, customerName, email, phoneNumber,
                    address, city, country, paymentMethod,
                    createdAt, updatedAt
            );
        } catch (Exception e) {
            log.error("Error converting DynamoDB item to CustomerInfo", e);
            throw new RuntimeException("Failed to reconstruct CustomerInfo from DynamoDB", e);
        }
    }
}
