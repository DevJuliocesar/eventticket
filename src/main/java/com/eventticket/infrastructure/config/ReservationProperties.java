package com.eventticket.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for reservation settings.
 * Maps to application.reservation.* properties.
 * Spring Boot automatically maps kebab-case (timeout-minutes) to camelCase (timeoutMinutes).
 */
@Component
@ConfigurationProperties(prefix = "application.reservation")
public class ReservationProperties {

    private static final Logger log = LoggerFactory.getLogger(ReservationProperties.class);

    private int timeoutMinutes = 10; // Default value
    private long checkIntervalMs = 60000; // Default: 1 minute

    @PostConstruct
    public void logConfiguration() {
        log.info("Reservation configuration loaded - timeout: {} minutes, checkInterval: {} ms", 
                timeoutMinutes, checkIntervalMs);
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public long getCheckIntervalMs() {
        return checkIntervalMs;
    }

    public void setCheckIntervalMs(long checkIntervalMs) {
        this.checkIntervalMs = checkIntervalMs;
    }
}
