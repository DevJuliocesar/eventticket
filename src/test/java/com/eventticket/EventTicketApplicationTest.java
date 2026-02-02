package com.eventticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "aws.dynamodb.endpoint=http://localhost:4566",
        "aws.sqs.endpoint=http://localhost:4566",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
@DisplayName("EventTicketApplication Integration Tests")
class EventTicketApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Should load Spring application context successfully")
    void shouldLoadSpringApplicationContextSuccessfully() {
        // Then
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("Should have DynamoDB client bean")
    void shouldHaveDynamoDBClientBean() {
        // When
        DynamoDbAsyncClient dynamoDbClient = applicationContext.getBean(DynamoDbAsyncClient.class);

        // Then
        assertThat(dynamoDbClient).isNotNull();
    }

    @Test
    @DisplayName("Should have SQS client bean")
    void shouldHaveSqsClientBean() {
        // When
        SqsAsyncClient sqsClient = applicationContext.getBean(SqsAsyncClient.class);

        // Then
        assertThat(sqsClient).isNotNull();
    }

    @Test
    @DisplayName("Should have EventTicketApplication bean")
    void shouldHaveEventTicketApplicationBean() {
        // When
        EventTicketApplication app = applicationContext.getBean(EventTicketApplication.class);

        // Then
        assertThat(app).isNotNull();
    }

    @Test
    @DisplayName("Should have all required configuration beans")
    void shouldHaveAllRequiredConfigurationBeans() {
        // Then
        assertThat(applicationContext.getBean("dynamoDbAsyncClient")).isNotNull();
        assertThat(applicationContext.getBean("sqsAsyncClient")).isNotNull();
        assertThat(applicationContext.getBean("objectMapper")).isNotNull();
    }

    @Test
    @DisplayName("Should have main method that can be called")
    void shouldHaveMainMethodThatCanBeCalled() {
        // Given
        String[] args = new String[]{};

        // When & Then - Should not throw exception
        // Note: We don't actually call main() in unit tests as it starts the full application
        // This test just verifies the method exists and is accessible
        assertThat(EventTicketApplication.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("main") && 
                        method.getParameterCount() == 1 &&
                        method.getParameterTypes()[0] == String[].class);
    }
}
