package com.eventticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the EventTicket application.
 * This application implements a reactive ticketing system using Clean Architecture principles.
 */
@SpringBootApplication
public class EventTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventTicketApplication.class, args);
    }
}
