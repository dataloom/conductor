package com.kryptnostic.metrics.v1;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.collect.Maps;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.kryptnostic.instrumentation.v1.constants.MetricsField;
import com.kryptnostic.mapstores.v1.constants.HazelcastNames;

public class MetricsElasticsearchHandler extends Handler {
    private Client                        client;
    private MetricsTransportClientFactory factory;
    private boolean                       connected = true;
    private IQueue<MetricsLogRecord>      log_records;
    private String                        server;
    private String                        cluster;

    public MetricsElasticsearchHandler( MetricsConfiguration config, HazelcastInstance hazelcastInstance ) {
        init( config, hazelcastInstance );
        client = factory.getClient();
    }

    public MetricsElasticsearchHandler(
            MetricsConfiguration config,
            HazelcastInstance hazelcastInstance,
            Client someClient ) {
        init( config, hazelcastInstance );
        client = someClient;
    }

    private void init( MetricsConfiguration config, HazelcastInstance hazelcastInstance ) {
        server = config.getElasticsearchUrl().get();
        cluster = config.getElasticsearchCluster().get();
        log_records = hazelcastInstance.getQueue( HazelcastNames.Queues.LOG_RECORDS );
        factory = new MetricsTransportClientFactory( server, 9300, false, cluster );
    }

    public void publishToElasticsearch( MetricsLogRecord record ) {
        if ( !connected ) {
            saveToDB( record );
        } else if ( verifyElasticsearchConnection() ) {
            saveToElasticsearch( record );
        } else {
            saveToDB( record );
        }
    }

    private void saveToElasticsearch( MetricsLogRecord record ) {
        Map<MetricsField, Object> enumMap = record.getMap();
        Map<String, Object> elasticMap = Maps.newHashMap();
        for ( Map.Entry<MetricsField, Object> entry : enumMap.entrySet() ) {
            elasticMap.put( entry.getKey().name().toLowerCase(), entry.getValue() );
        }
        client.prepareIndex( record.getIndex().name().toLowerCase(), record.getType().name().toLowerCase() )
                .setSource( elasticMap ).execute().actionGet();
    }

    @Override
    public void flush() {
        // flush taken care of by publishToElasticsearch; never called
    }

    @Override
    public void close() throws SecurityException {
        client.close();

    }

    @Override
    public void publish( LogRecord record ) {
        throw new ElasticsearchException( "Didn't publish to elasticsearch" );

    }

    public void saveToDB( MetricsLogRecord record ) {
        log_records.add( record );
    }

    private void flushToElasticsearch() {
        Collection<MetricsLogRecord> c = new BlockingArrayQueue<MetricsLogRecord>();
        log_records.drainTo( c );
        for ( MetricsLogRecord record : c ) {
            record = log_records.poll();
            saveToElasticsearch( record );
        }
    }

    public boolean verifyElasticsearchConnection() {
        if ( connected ) {
            if ( factory.verifyConnection( client ) ) {
                flushToElasticsearch();
            } else {
                connected = false;
            }
        } else {
            client = factory.getClient();
            if ( client != null ) {
                connected = true;
            } else {
                connected = false;
            }
        }
        return connected;
    }

    @Scheduled(
        fixedRate = 1800000 )
    public void verifyRunner() {
        verifyElasticsearchConnection();
    }

}
