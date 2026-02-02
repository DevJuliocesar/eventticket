package com.eventticket.infrastructure.repository;

import com.eventticket.domain.model.CustomerInfo;
import com.eventticket.domain.valueobject.CustomerId;
import com.eventticket.domain.valueobject.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.paginators.ScanPublisher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DynamoDBCustomerInfoRepository Tests")
class DynamoDBCustomerInfoRepositoryTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @InjectMocks
    private DynamoDBCustomerInfoRepository customerInfoRepository;

    private CustomerInfo testCustomerInfo;
    private OrderId orderId;
    private CustomerId customerId;

    @BeforeEach
    void setUp() {
        orderId = OrderId.generate();
        customerId = CustomerId.of("customer-123");
        testCustomerInfo = CustomerInfo.create(
                customerId, orderId, "John Doe", "john@example.com", "1234567890",
                null, null, null, null
        );
    }


    @Test
    @DisplayName("Should save customer info successfully")
    void shouldSaveCustomerInfoSuccessfully() {
        // Given
        PutItemResponse putResponse = PutItemResponse.builder().build();
        CompletableFuture<PutItemResponse> future = CompletableFuture.completedFuture(putResponse);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<CustomerInfo> result = customerInfoRepository.save(testCustomerInfo);

        // Then
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer.getOrderId()).isEqualTo(orderId);
                    assertThat(customer.getCustomerId()).isEqualTo(customerId);
                    assertThat(customer.getCustomerName()).isEqualTo("John Doe");
                    assertThat(customer.getEmail()).isEqualTo("john@example.com");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find customer info by order ID when exists")
    void shouldFindCustomerInfoByOrderIdWhenExists() {
        // Given
        Map<String, AttributeValue> item = createCustomerInfoItem(testCustomerInfo);
        GetItemResponse getResponse = GetItemResponse.builder()
                .item(item)
                .build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<CustomerInfo> result = customerInfoRepository.findByOrderId(orderId);

        // Then
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer.getOrderId()).isEqualTo(orderId);
                    assertThat(customer.getCustomerId()).isEqualTo(customerId);
                    assertThat(customer.getCustomerName()).isEqualTo("John Doe");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when customer info not found")
    void shouldReturnEmptyWhenCustomerInfoNotFound() {
        // Given
        GetItemResponse getResponse = GetItemResponse.builder().build();
        CompletableFuture<GetItemResponse> future = CompletableFuture.completedFuture(getResponse);

        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<CustomerInfo> result = customerInfoRepository.findByOrderId(orderId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find customer info by customer ID")
    void shouldFindCustomerInfoByCustomerId() {
        // Given
        Map<String, AttributeValue> item = createCustomerInfoItem(testCustomerInfo);
        ScanResponse scanResponse = ScanResponse.builder()
                .items(item)
                .build();

        ScanPublisher scanPublisher = Mockito.mock(ScanPublisher.class);
        Publisher<ScanResponse> publisher = Flux.just(scanResponse);
        doAnswer(invocation -> {
            Subscriber<? super ScanResponse> subscriber = (Subscriber<? super ScanResponse>) invocation.getArgument(0);
            publisher.subscribe(subscriber);
            return null;
        }).when(scanPublisher).subscribe(any(Subscriber.class));
        
        when(dynamoDbClient.scanPaginator(any(ScanRequest.class)))
                .thenReturn(scanPublisher);

        // When
        Flux<CustomerInfo> result = customerInfoRepository.findByCustomerId(customerId);

        // Then
        StepVerifier.create(result)
                .assertNext(customer -> {
                    assertThat(customer.getCustomerId()).isEqualTo(customerId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should delete customer info by order ID")
    void shouldDeleteCustomerInfoByOrderId() {
        // Given
        DeleteItemResponse deleteResponse = DeleteItemResponse.builder().build();
        CompletableFuture<DeleteItemResponse> future = CompletableFuture.completedFuture(deleteResponse);

        when(dynamoDbClient.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(future);

        // When
        Mono<Void> result = customerInfoRepository.deleteByOrderId(orderId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    private Map<String, AttributeValue> createCustomerInfoItem(CustomerInfo customerInfo) {
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
}
