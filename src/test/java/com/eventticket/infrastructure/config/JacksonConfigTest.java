package com.eventticket.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacksonConfig Tests")
class JacksonConfigTest {

    private JacksonConfig jacksonConfig;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        jacksonConfig = new JacksonConfig();
    }

    @Test
    @DisplayName("Should create ObjectMapper with JavaTimeModule")
    void shouldCreateObjectMapperWithJavaTimeModule() {
        // When
        ObjectMapper mapper = jacksonConfig.objectMapper();

        // Then
        assertThat(mapper).isNotNull();
        // JavaTimeModule is registered with ID "jackson-datatype-jsr310"
        assertThat(mapper.getRegisteredModuleIds()).contains("jackson-datatype-jsr310");
    }

    @Test
    @DisplayName("Should configure FAIL_ON_UNKNOWN_PROPERTIES as false")
    void shouldConfigureFailOnUnknownPropertiesAsFalse() {
        // When
        ObjectMapper mapper = jacksonConfig.objectMapper();

        // Then
        assertThat(mapper.getDeserializationConfig()
                .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
    }

    @Test
    @DisplayName("Should serialize Instant correctly")
    void shouldSerializeInstantCorrectly() throws Exception {
        // Given
        ObjectMapper mapper = jacksonConfig.objectMapper();
        Instant instant = Instant.now();

        // When
        String json = mapper.writeValueAsString(instant);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).isNotBlank();
    }

    @Test
    @DisplayName("Should deserialize Instant correctly")
    void shouldDeserializeInstantCorrectly() throws Exception {
        // Given
        ObjectMapper mapper = jacksonConfig.objectMapper();
        String json = "\"2024-01-01T00:00:00Z\"";

        // When
        Instant instant = mapper.readValue(json, Instant.class);

        // Then
        assertThat(instant).isNotNull();
        assertThat(instant.toString()).isEqualTo("2024-01-01T00:00:00Z");
    }

    @Test
    @DisplayName("Should ignore unknown properties during deserialization")
    void shouldIgnoreUnknownPropertiesDuringDeserialization() throws Exception {
        // Given
        ObjectMapper mapper = jacksonConfig.objectMapper();
        String json = "{\"knownProperty\":\"value\",\"unknownProperty\":\"ignored\"}";

        // When & Then - Should not throw exception
        TestRecord record = mapper.readValue(json, TestRecord.class);
        assertThat(record.knownProperty()).isEqualTo("value");
    }

    @Test
    @DisplayName("Should create primary ObjectMapper bean")
    void shouldCreatePrimaryObjectMapperBean() {
        // When
        ObjectMapper mapper = jacksonConfig.objectMapper();

        // Then
        assertThat(mapper).isNotNull();
        // The @Primary annotation is verified by Spring context, not by unit test
    }

    // Helper record for testing unknown properties
    private record TestRecord(String knownProperty) {
    }
}
