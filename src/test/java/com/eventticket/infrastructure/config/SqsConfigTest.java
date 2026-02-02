package com.eventticket.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqsConfig Tests")
class SqsConfigTest {

    private SqsConfig sqsConfig;

    @BeforeEach
    void setUp() {
        sqsConfig = new SqsConfig();
        ReflectionTestUtils.setField(sqsConfig, "region", "us-east-1");
        ReflectionTestUtils.setField(sqsConfig, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(sqsConfig, "secretAccessKey", "test-secret");
        ReflectionTestUtils.setField(sqsConfig, "sqsEndpoint", "http://localhost:4566");
    }

    @Test
    @DisplayName("Should create SQS client with default values")
    void shouldCreateSqsClientWithDefaultValues() {
        // When
        SqsAsyncClient client = sqsConfig.sqsAsyncClient();

        // Then
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create SQS client with custom region")
    void shouldCreateSqsClientWithCustomRegion() {
        // Given
        ReflectionTestUtils.setField(sqsConfig, "region", "us-west-2");

        // When
        SqsAsyncClient client = sqsConfig.sqsAsyncClient();

        // Then
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create SQS client with custom endpoint")
    void shouldCreateSqsClientWithCustomEndpoint() {
        // Given
        ReflectionTestUtils.setField(sqsConfig, "sqsEndpoint", "http://custom-endpoint:4566");

        // When
        SqsAsyncClient client = sqsConfig.sqsAsyncClient();

        // Then
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create SQS client with custom credentials")
    void shouldCreateSqsClientWithCustomCredentials() {
        // Given
        ReflectionTestUtils.setField(sqsConfig, "accessKeyId", "custom-key");
        ReflectionTestUtils.setField(sqsConfig, "secretAccessKey", "custom-secret");

        // When
        SqsAsyncClient client = sqsConfig.sqsAsyncClient();

        // Then
        assertThat(client).isNotNull();
        client.close();
    }

    @Test
    @DisplayName("Should create multiple clients independently")
    void shouldCreateMultipleClientsIndependently() {
        // When
        SqsAsyncClient client1 = sqsConfig.sqsAsyncClient();
        SqsAsyncClient client2 = sqsConfig.sqsAsyncClient();

        // Then
        assertThat(client1).isNotNull();
        assertThat(client2).isNotNull();
        assertThat(client1).isNotSameAs(client2);
        client1.close();
        client2.close();
    }
}
