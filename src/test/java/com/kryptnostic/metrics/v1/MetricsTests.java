package com.kryptnostic.metrics.v1;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.joda.time.LocalDate;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekbeast.rhizome.configuration.service.ConfigurationService;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.kryptnostic.heracles.directory.v1.objects.Emails;
import com.kryptnostic.heracles.directory.v1.services.DirectoryService;
import com.kryptnostic.instrumentation.v1.DefaultLoggingClient;
import com.kryptnostic.instrumentation.v1.constants.MetricsField;
import com.kryptnostic.instrumentation.v1.constants.MetricsIndex;
import com.kryptnostic.instrumentation.v1.constants.MetricsType;
import com.kryptnostic.instrumentation.v1.models.MetricsMetadata;
import com.kryptnostic.instrumentation.v1.models.MetricsObject;
import com.kryptnostic.instrumentation.v1.utils.MetricsObjectMapper;
import com.kryptnostic.mapstores.v1.constants.HazelcastNames;
import com.kryptnostic.metrics.v1.userstats.MetricsDate;
import com.kryptnostic.metrics.v1.userstats.MetricsUserStatsHandler;

@SuppressWarnings( "javadoc" )
public class MetricsTests extends HazelcastTestHarness {
    private static DefaultLoggingClient client;
    private static Client               esClient;
    private static final String         DOMAIN          = "testStats";
    private static final UUID           TEST_UUID       = UUID.randomUUID();
    private static final String         HANDLER_MESSAGE = "testing MetricsElasticsearchHandler";
    private static final String         LOGGER_MESSAGE  = "testing MetricsLogger";
    private static final String         SERVICE_MESSAGE = "testing LoggingMetricsService";

    private static final Logger         logger          = LoggerFactory.getLogger( MetricsTests.class );

    private static String               ELASTICSEARCH_CLUSTER;

    // hardcoded since this test runs the server locally
    private static final String         localhostURL    = "http://localhost:8084/v1";
    private static Node                 node;

    protected static MetricsServer      metrics         = new MetricsServer();

    @BeforeClass
    public static void initTests() throws Exception {
        MetricsConfiguration metricsConfig = ConfigurationService.StaticLoader
                .loadConfiguration( MetricsConfiguration.class );
        ELASTICSEARCH_CLUSTER = metricsConfig.getElasticsearchCluster().get();

        node = NodeBuilder
                .nodeBuilder()
                .settings( ImmutableSettings.builder().put( "shield.enabled", false )
                        .put( "cluster.name", ELASTICSEARCH_CLUSTER ) )
                .local( true ).node();
        node.start();
        esClient = node.client();
        metrics.start();

        client = new DefaultLoggingClient( Optional.of( localhostURL ) );

        if ( esClient.admin().indices().prepareExists( MetricsIndex.TEST.name().toLowerCase() ).execute()
                .actionGet().isExists() ) {
            esClient.admin().indices().prepareDelete( MetricsIndex.TEST.name().toLowerCase() ).execute()
                    .actionGet();
        }

    }

    @AfterClass
    public static void tearDownTests() throws BeansException, Exception {
        client.shutdownClient();
        node.close();
        metrics.stop();
    }

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks( this );
    }

    @Test
    public void testMetricsElasticsearchHandlerHazelcastQueue() {
        String logMessage = "testing elasticsearch handler queueing to db";
        MetricsLogRecord record = new MetricsLogRecord(
                logMessage,
                new HashMap<MetricsField, Object>(),
                MetricsIndex.TEST,
                MetricsType.TEST,
                TEST_UUID.toString() );

        MetricsConfiguration mockedConfig = Mockito.mock( MetricsConfiguration.class );
        Mockito.when( mockedConfig.getElasticsearchUrl() ).thenReturn( Optional.of( localhostURL ) );
        Mockito.when( mockedConfig.getElasticsearchCluster() ).thenReturn( Optional.of( ELASTICSEARCH_CLUSTER ) );

        MetricsElasticsearchHandler meh = new MetricsElasticsearchHandler( mockedConfig, hazelcast, null );
        meh.publishToElasticsearch( record );

        IQueue<MetricsLogRecord> log_records = hazelcast.getQueue( HazelcastNames.Queues.LOG_RECORDS );
        Assert.assertTrue( log_records.contains( record ) );
        log_records.poll();

    }

    @Test
    public void testMetricsUserStatsHandler() {
        UUID userstatsTestUUID = UUID.randomUUID();
        IMap<LocalDate, Integer> date_count = hazelcast.getMap( HazelcastNames.Maps.DATE_COUNT );
        IMap<LocalDate, String> date_users = hazelcast.getMap( HazelcastNames.Maps.DATE_USERS );
        IMap<String, MetricsUserStatsMetadata> dateuser_data = hazelcast.getMap( HazelcastNames.Maps.DATEUSER_DATA );

        LocalDate date = MetricsDate.getDate();
        int hits_today = date_count.getOrDefault( date, 0 );

        String logMessage = "testing user stats handler";
        HashMap<MetricsField, Object> map = Maps.newHashMap();
        map.put( MetricsField.USER_UUID_FIELD, TEST_UUID );
        map.put( MetricsField.DOMAIN_FIELD, DOMAIN );
        MetricsLogRecord record = new MetricsLogRecord(
                logMessage,
                map,
                MetricsIndex.TEST,
                MetricsType.TEST,
                userstatsTestUUID.toString() );

        MetricsUserStatsHandler mush = new MetricsUserStatsHandler( hazelcast );
        MetricsUserStatsMetadata metadata = new MetricsUserStatsMetadata( DOMAIN );
        mush.publishToDB( record, metadata );

        Assert.assertTrue( hits_today + 1 == date_count.get( date ) );
        Assert.assertTrue( userstatsTestUUID.toString().equals( date_users.get( date ) ) );
        Assert.assertTrue( metadata.equals( dateuser_data.get( MetricsDate.getDay( date )
                + userstatsTestUUID.toString() ) ) );

    }

    @Test
    public void metricsElasticsearchHandlerTest() {
        MetricsConfiguration mockedConfig = Mockito.mock( MetricsConfiguration.class );
        Mockito.when( mockedConfig.getElasticsearchUrl() ).thenReturn( Optional.of( localhostURL ) );
        Mockito.when( mockedConfig.getElasticsearchCluster() ).thenReturn( Optional.of( ELASTICSEARCH_CLUSTER ) );

        MetricsElasticsearchHandler handler = new MetricsElasticsearchHandler( mockedConfig, hazelcast, esClient );

        Map<MetricsField, Object> map = Maps.newHashMap();
        map.put( MetricsField.USER_UUID_FIELD, TEST_UUID.toString() );
        MetricsLogRecord log = new MetricsLogRecord(
                HANDLER_MESSAGE,
                map,
                MetricsIndex.TEST,
                MetricsType.TEST,
                TEST_UUID.toString() );
        try {
            handler.publishToElasticsearch( log );
        } catch ( ElasticsearchException e ) {
            logger.error( "Failed to publish to Elasticsearch: ", e );
            Assert.fail();
        }
        esClient.admin().indices().prepareRefresh( MetricsIndex.TEST.name().toLowerCase() ).execute()
                .actionGet();
        Assert.assertTrue( esClient.prepareSearch( MetricsIndex.TEST.name().toLowerCase() )
                .setTypes( MetricsType.TEST.name().toLowerCase() ).execute().actionGet()
                .getHits().getTotalHits() == 1 );

    }

    @Test
    public void metricsLoggerTest() {
        Map<MetricsField, Object> map = Maps.newHashMap();
        map.put( MetricsField.USER_UUID_FIELD, TEST_UUID.toString() );
        String testEmail = "test@kryptnostic.com";
        String testUser = "test";
        String testDomain = "kryptnostic.com";

        MetricsElasticsearchHandler meh = Mockito.mock( MetricsElasticsearchHandler.class );
        MetricsUserStatsHandler mush = Mockito.mock( MetricsUserStatsHandler.class );
        DirectoryService directoryService = Mockito.mock( DirectoryService.class );
        Mockito.when( directoryService.getEmails( TEST_UUID ) ).thenReturn( new Emails( Arrays.asList( testEmail ) ) );
        Mockito.when( directoryService.getEmails( TEST_UUID ) ).thenReturn( new Emails( Arrays.asList( testEmail ) ) );
        Mockito.when( directoryService.getName( TEST_UUID ) ).thenReturn( testUser );

        MetricsLogger metricsLogger = new MetricsLogger( meh, mush, directoryService );

        try {
            metricsLogger.logMetrics( LOGGER_MESSAGE,
                    map,
                    MetricsIndex.TEST,
                    MetricsType.TEST );
        } catch ( ElasticsearchException e ) {
            logger.error( "Couldn't log: ", e );
        }

        map.put( MetricsField.EMAIL_FIELD, testEmail );
        map.put( MetricsField.USER_NAME_FIELD, testUser );
        map.put( MetricsField.DOMAIN_FIELD, testDomain );
        MetricsLogRecord record = new MetricsLogRecord(
                LOGGER_MESSAGE,
                map,
                MetricsIndex.TEST,
                MetricsType.TEST,
                TEST_UUID.toString() );
        MetricsUserStatsMetadata metadata = new MetricsUserStatsMetadata( testDomain );

        Mockito.verify( meh, Mockito.atLeastOnce() ).publishToElasticsearch( Matchers.refEq( record ) );
        Mockito.verify( mush, Mockito.atLeastOnce() ).publishToDB( Matchers.refEq( record ),
                Matchers.refEq( metadata ) );

    }

    @Test
    public void loggingMetricsServiceTest() {
        Map<MetricsField, Object> map = Maps.newHashMap();
        map.put( MetricsField.USER_UUID_FIELD, TEST_UUID.toString() );

        MetricsMetadata metricsMetadata = new MetricsMetadata(
                map,
                SERVICE_MESSAGE,
                MetricsIndex.TEST,
                MetricsType.TEST );
        MetricsObject object = new MetricsObject( metricsMetadata );

        MetricsLogger mockedMetricsLogger = Mockito.mock( MetricsLogger.class );
        LoggingMetricsService loggingService = new LoggingMetricsService( mockedMetricsLogger );

        loggingService.log( object );

        try {
            Mockito.verify( mockedMetricsLogger ).logMetrics(
                    metricsMetadata.getMessage(),
                    metricsMetadata.getMap(),
                    metricsMetadata.getIndex(),
                    metricsMetadata.getType() );
        } catch ( ElasticsearchException e ) {
            logger.error( "Error while verifying: ", e );
        }

    }

    @Test
    public void checkMetricsUserStatsMetadataSerialization() throws IOException {
        String testDomain = "kryptnostic.com";
        MetricsUserStatsMetadata metadata = new MetricsUserStatsMetadata( testDomain );

        ObjectMapper mapper = MetricsObjectMapper.getObjectMapper();
        String serialized = mapper.writeValueAsString( metadata );
        Assert.assertNotNull( serialized );
        MetricsUserStatsMetadata recovered = mapper.readValue( serialized, MetricsUserStatsMetadata.class );
        Assert.assertTrue( metadata.equals( recovered ) );

    }

}
