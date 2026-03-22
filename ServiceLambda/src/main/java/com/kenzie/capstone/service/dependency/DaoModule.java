package com.kenzie.capstone.service.dependency;

import com.kenzie.capstone.service.dao.EventDao;
import com.kenzie.capstone.service.util.DynamoDbClientProvider;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import dagger.Module;
import dagger.Provides;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides AWS SDK v2 DynamoDbEnhancedClient and DAO instances via Dagger.
     * Replaces the legacy SDK v1 DynamoDBMapper wiring.
     */
@Module
    public class DaoModule {

    @Singleton
            @Provides
            @Named("DynamoDbEnhancedClient")
            public DynamoDbEnhancedClient provideDynamoDbEnhancedClient() {
                        return DynamoDbClientProvider.getEnhancedClient();
            }

    @Singleton
            @Provides
            @Named("EventDao")
            @Inject
            public EventDao provideEventDao(@Named("DynamoDbEnhancedClient") DynamoDbEnhancedClient enhancedClient) {
                        return new EventDao(enhancedClient);
            }
    }
