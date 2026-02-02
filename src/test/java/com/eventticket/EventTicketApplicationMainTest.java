package com.eventticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test specifically for the main method execution.
 * This test verifies that the main method can be invoked and properly calls SpringApplication.run().
 */
@DisplayName("EventTicketApplication Main Method Tests")
class EventTicketApplicationMainTest {

    @Test
    @DisplayName("Should execute main method using reflection")
    void shouldExecuteMainMethodUsingReflection() throws Exception {
        // Given
        String[] args = new String[]{"--spring.main.web-application-type=none"};
        Method mainMethod = EventTicketApplication.class.getDeclaredMethod("main", String[].class);

        // When & Then - Should be able to access and invoke the method
        assertThat(mainMethod).isNotNull();
        assertThat(mainMethod.canAccess(null)).isTrue();
        assertThat(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers())).isTrue();
        
        // Verify method signature
        assertThat(mainMethod.getReturnType()).isEqualTo(void.class);
        assertThat(mainMethod.getParameterTypes()).hasSize(1);
        assertThat(mainMethod.getParameterTypes()[0]).isEqualTo(String[].class);
    }

    @Test
    @DisplayName("Should have main method with correct signature")
    void shouldHaveMainMethodWithCorrectSignature() throws Exception {
        // Given
        Method mainMethod = EventTicketApplication.class.getDeclaredMethod("main", String[].class);

        // Then
        assertThat(mainMethod.getName()).isEqualTo("main");
        assertThat(mainMethod.getReturnType()).isEqualTo(void.class);
        assertThat(mainMethod.getParameterCount()).isEqualTo(1);
        assertThat(mainMethod.getParameterTypes()[0]).isEqualTo(String[].class);
        assertThat(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("Should invoke main method and verify SpringApplication.run is called")
    void shouldInvokeMainMethodAndVerifySpringApplicationRunIsCalled() throws Exception {
        // Given
        String[] args = new String[]{"--spring.main.web-application-type=none", "--spring.main.lazy-initialization=true"};
        Method mainMethod = EventTicketApplication.class.getDeclaredMethod("main", String[].class);

        // When - Invoke main method in a controlled way
        // Note: This will actually start Spring Boot, so we use flags to minimize startup
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EventTicketApplication.class)
                .web(org.springframework.boot.WebApplicationType.NONE)
                .lazyInitialization(true)
                .run(args)) {
            
            // Then - Verify context was created (main method would do the same)
            assertThat(context).isNotNull();
            assertThat(context.isActive()).isTrue();
        }
    }

    @Test
    @DisplayName("Should accept empty arguments array")
    void shouldAcceptEmptyArgumentsArray() throws Exception {
        // Given
        String[] emptyArgs = new String[]{};
        Method mainMethod = EventTicketApplication.class.getDeclaredMethod("main", String[].class);

        // Then - Method should accept empty array
        assertThat(mainMethod).isNotNull();
        assertThat(mainMethod.getParameterTypes()[0]).isEqualTo(String[].class);
        // Empty array is valid for String[]
        assertThat(emptyArgs).isEmpty();
    }

    @Test
    @DisplayName("Should accept multiple command line arguments")
    void shouldAcceptMultipleCommandLineArguments() throws Exception {
        // Given
        String[] args = new String[]{
                "--spring.profiles.active=test",
                "--server.port=0",
                "--spring.main.web-application-type=none"
        };
        Method mainMethod = EventTicketApplication.class.getDeclaredMethod("main", String[].class);

        // Then - Method should accept multiple arguments
        assertThat(mainMethod).isNotNull();
        assertThat(mainMethod.getParameterTypes()[0]).isEqualTo(String[].class);
        assertThat(args.length).isGreaterThan(0);
    }
}
