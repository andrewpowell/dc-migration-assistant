package com.aws.apn.migration.awsmigrationplugin.core;

import com.aws.apn.migration.awsmigrationplugin.spi.MigrationService;
import com.aws.apn.migration.awsmigrationplugin.spi.MigrationStage;

import static com.aws.apn.migration.awsmigrationplugin.spi.MigrationStage.STARTED;
import static com.aws.apn.migration.awsmigrationplugin.spi.MigrationStage.UNSTARTED;

/**
 * Manages a migration from on-premise to self-hosted AWS.
 */
public class AWSMigrationService implements MigrationService {

    private MigrationStage currentStage;

    /**
     * Creates a new, unstarted AWS Migration
     */
    public AWSMigrationService() {
        this.currentStage = UNSTARTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startMigration() {
        this.currentStage = STARTED;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MigrationStage getMigrationStage() {
        return currentStage;
    }
}
