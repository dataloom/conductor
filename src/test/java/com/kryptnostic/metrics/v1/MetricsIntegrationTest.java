package com.kryptnostic.metrics.v1;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import com.geekbeast.rhizome.configuration.service.ConfigurationService;
import com.google.common.base.Optional;
import com.kryptnostic.instrumentation.v1.DefaultLoggingClient;
import com.kryptnostic.instrumentation.v1.constants.MetricsField;
import com.kryptnostic.instrumentation.v1.constants.MetricsIndex;
import com.kryptnostic.instrumentation.v1.constants.MetricsType;
import com.kryptnostic.instrumentation.v1.models.MetricsRequest;
import com.kryptnostic.instrumentation.v1.models.MetricsRequest.MetricsRequestBuilder;

//Until we can come up with a way to run the elasticsearch server from within the integration test, this test is @Ignored so that the builds don't break if elasticsearch isn't running on batman.
//I'm leaving this test in, however, because it's an excellent sanity check during the debugging process, when elasticsearch can be run locally on the developer's machine.
@Ignore
public class MetricsIntegrationTest {

    private static DefaultLoggingClient client;
    private static Client               transportClient;
    private static String               ELASTICSEARCH_URL;
    private static String               ELASTICSEARCH_CLUSTER;
    private static final Logger         logger       = LoggerFactory
                                                             .getLogger( MetricsIntegrationTest.class );
    private static final UUID           TEST_UUID    = UUID.randomUUID();

    // hardcoded since this test runs the server locally
    private static final String         localhostURL = "http://localhost:8084/v1";
    protected static MetricsServer      metrics      = new MetricsServer();

    @SuppressWarnings( "resource" )
    @BeforeClass
    public static void initTests() throws Exception {
        metrics.start();
        client = new DefaultLoggingClient( Optional.of( localhostURL ) );
        MetricsConfiguration metricsConfig = ConfigurationService.StaticLoader
                .loadConfiguration( MetricsConfiguration.class );
        ELASTICSEARCH_URL = metricsConfig.getElasticsearchUrl().get();
        ELASTICSEARCH_CLUSTER = metricsConfig.getElasticsearchCluster().get();
        Settings settings = ImmutableSettings.settingsBuilder().put( "cluster.name", ELASTICSEARCH_CLUSTER ).build();
        transportClient = new TransportClient( settings ).addTransportAddress( new InetSocketTransportAddress(
                ELASTICSEARCH_URL,
                9300 ) );
        if ( transportClient.admin().indices().prepareExists( MetricsIndex.TEST.name().toLowerCase() )
                .execute().actionGet().isExists() ) {
            transportClient.admin().indices().prepareDelete( MetricsIndex.TEST.name().toLowerCase() ).execute()
                    .actionGet();
        }
    }

    @AfterClass
    public static void tearDownTests() throws BeansException, Exception {
        client.shutdownClient();
        metrics.stop();
    }

    @Test
    public void testCompleteUploadProcess() {
        String logMessage = "testing the complete process of creating a message and uploading it to the server";
        MetricsRequest request = new MetricsRequestBuilder().message( logMessage )
                .type( MetricsType.TEST )
                .index( MetricsIndex.TEST )
                .addField( MetricsField.USER_UUID_FIELD, TEST_UUID )
                .build();
        Future<?> done = client.uploadObject( request );
        try {
            done.get();
        } catch ( InterruptedException | ExecutionException e ) {
            logger.error( "Thread interrupted: ", e );
            Assert.fail();
        }
        transportClient.admin().indices().prepareRefresh( MetricsIndex.TEST.name().toLowerCase() ).execute()
                .actionGet();
        Assert.assertTrue( transportClient.prepareSearch( MetricsIndex.TEST.name().toLowerCase() )
                .setTypes( MetricsType.TEST.name().toLowerCase() ).execute().actionGet().getHits()
                .getTotalHits() == 1 );
    }

}
