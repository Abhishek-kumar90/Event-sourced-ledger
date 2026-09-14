package com.example.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration.
 *
 * @param snapshot snapshotting settings
 */
@ConfigurationProperties(prefix = "ledger")
public record AppProperties(Snapshot snapshot) {

    /**
     * @param every take a snapshot of an account every N events (0 disables snapshots).
     *              Snapshots let us rebuild current state without replaying the entire
     *              history from the beginning.
     */
    public record Snapshot(int every) {
    }
}
