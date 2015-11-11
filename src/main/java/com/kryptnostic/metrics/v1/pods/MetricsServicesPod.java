package com.kryptnostic.metrics.v1.pods;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekbeast.rhizome.configuration.service.ConfigurationService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.heracles.authentication.v1.mapstores.HazelcastDirectoryService;
import com.kryptnostic.heracles.directory.v1.services.DirectoryService;
import com.kryptnostic.kodex.v1.serialization.jackson.KodexObjectMapperFactory;
import com.kryptnostic.metrics.v1.LoggingMetricsService;
import com.kryptnostic.metrics.v1.MetricsConfiguration;
import com.kryptnostic.metrics.v1.MetricsElasticsearchHandler;
import com.kryptnostic.metrics.v1.serialization.MetricsLogRecordStreamSerializer;
import com.kryptnostic.metrics.v1.serialization.MetricsUserStatsMetadataStreamSerializer;
import com.kryptnostic.metrics.v1.userstats.MetricsUserStatsHandler;

public class MetricsServicesPod {

    @Inject
    private ConfigurationService config;

    @Inject
    private HazelcastInstance    hazelcastInstance;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return KodexObjectMapperFactory.getObjectMapper();
    }

    @Bean
    public MetricsLogRecordStreamSerializer metricsLogRecordStreamSerializer() {
        return new MetricsLogRecordStreamSerializer();
    }

    @Bean
    public MetricsUserStatsMetadataStreamSerializer metricsUserStatsMetadataStreamSerializer() {
        return new MetricsUserStatsMetadataStreamSerializer();
    }

    @Bean
    public MetricsConfiguration metricsConfiguration() throws IOException {
        return config.getConfiguration( MetricsConfiguration.class );
    }

    @Bean
    public LoggingMetricsService loggingMetricsService() throws IOException {
        return new LoggingMetricsService( directoryService(), meh(), mush(), hazelcastInstance );
    }

    @Bean
    public MetricsElasticsearchHandler meh() throws IOException {
        return new MetricsElasticsearchHandler( metricsConfiguration(), hazelcastInstance );
    }

    @Bean
    public MetricsUserStatsHandler mush() {
        return new MetricsUserStatsHandler( hazelcastInstance );
    }

    @Bean
    public DirectoryService directoryService() {
        return new HazelcastDirectoryService( hazelcastInstance );
    }

}
