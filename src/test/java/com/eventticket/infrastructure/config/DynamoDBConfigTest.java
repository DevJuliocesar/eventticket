package com.eventticket.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DynamoDBConfig Tests")
class DynamoDBConfigTest {

    private DynamoDBConfig dynamoDBConfig;

    @BeforeEach
    void setUp() {
        dynamoDBConfig = new DynamoDBConfig();
        ReflectionTestUtils.setField(dynamoDBConfig, "region", "us-east-1");
        ReflectionTestUtils.setField(dynamoDBConfig, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(dynamoDBConfig, "secretAccessKey", "test-secret");
        ReflectionTestUtils.setField(dynamoDBConfig, "dynamoDbEndpoint", "http://localhost:4566");
    }

    @Test
    @DisplayName("Should create DynamoDB client with default values")
    void shouldCreateDynamoDBClientWithDefaultValues() {
        // When
        DynamoDbAsyncClient client = dynamoDBConfig.dynamoDbAsyncClient();

        // Then
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create DynamoDB client with custom region")
    void shouldCreateDynamoDBClientWithCustomRegion() {
        // Given
        ReflectionTestUtils.setField(dynamoDBConfig, "region", "us-west-2");

        // When
        DynamoDbAsyncClient client = dynamoDBConfig.dynamoDbAsyncClient();

        // Then
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create DynamoDB client with custom endpoint")
    void shouldCreateDynamoDBClientWithCustomEndpoint() {
        // Given
        ReflectionTestUtils.setField(dynamoDBConfig, "dynamoDbEndpoint", "http://custom-endpoint:4566");

        // When
        DynamoDbAsyncClient client = dynamoDBConfig.dynamoDbAsyncClient();

        // Then
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create DynamoDB client with custom credentials")
    void shouldCreateDynamoDBClientWithCustomCredentials() {
        // Given
        ReflectionTestUtils.setField(dynamoDBConfig, "accessKeyId", "custom-key");
        ReflectionTestUtils.setField(dynamoDBConfig, "secretAccessKey", "custom-secret");

        // When
        DynamoDbAsyncClient client = dynamoDBConfig.dynamoDbAsyncClient();

        // Then
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create multiple clients independently")
    void shouldCreateMultipleClientsIndependently() {
        // When
        DynamoDbAsyncClient client1 = dynamoDBConfig.dynamoDbAsyncClient();
        DynamoDbAsyncClient client2 = dynamoDBConfig.dynamoDbAsyncClient();

        // Then
        assertThat(client1).isNotNull();
        assertThat(client2).isNotNull();
        assertThat(client1).isNotSameAs(client2);
        client1.close();
        client2.close();
    }
}
