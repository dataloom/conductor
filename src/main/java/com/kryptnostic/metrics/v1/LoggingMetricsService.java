package com.kryptnostic.metrics.v1;

import java.io.IOException;

import org.elasticsearch.ElasticsearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.heracles.directory.v1.services.DirectoryService;
import com.kryptnostic.instrumentation.v1.models.MetricsMetadata;
import com.kryptnostic.instrumentation.v1.models.MetricsObject;
import com.kryptnostic.metrics.v1.userstats.MetricsUserStatsHandler;

public class LoggingMetricsService implements MetricsService {
    private static final String ERROR_COULD_NOT_LOG  = "Could not log to elasticsearch";
    private MetricsLogger       metricsLogger;
    private static final Logger logger               = LoggerFactory.getLogger( LoggingMetricsService.class );

    public LoggingMetricsService(
            DirectoryService directoryService,
            MetricsElasticsearchHandler meh,
            MetricsUserStatsHandler mush, 
            HazelcastInstance hazelcastInstance ) {
        metricsLogger = new MetricsLogger( hazelcastInstance, directoryService, meh , mush );
    }

    public LoggingMetricsService( MetricsLogger metricsLogger ) {
        this.metricsLogger = metricsLogger;
    }

    @Override
    public void log( MetricsObject met ) {
        try {
            log( met.getMetadata() );
        } catch ( IOException | ElasticsearchException e ) {
            logger.error( ERROR_COULD_NOT_LOG, e.getMessage() );
        }
    }

    @Timed
    private Optional<MetricsMetadata> log( MetricsMetadata metricsMetadata ) throws IOException, ElasticsearchException {
        metricsLogger.logMetrics(
                metricsMetadata.getMessage(),
                metricsMetadata.getMap(),
                metricsMetadata.getIndex(),
                metricsMetadata.getType() );
        return Optional.absent();
    }

}
