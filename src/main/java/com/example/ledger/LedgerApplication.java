package com.example.ledger;

import com.example.ledger.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Application entry point.
 *
 * <p>{@code SpringApplication.run(...)} boots the Spring container, which discovers the
 * event store, snapshot store, projection, service, and controller, wires them together,
 * and starts the embedded web server. Everything is in-memory, so there is nothing else
 * to install or run — no database, no broker, no Docker.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}
