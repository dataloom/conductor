package com.kryptnostic.conductor.pods;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.cassandra.CassandraSQLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.SparkContextJavaFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.rpc.ConductorConfiguration;
import com.kryptnostic.conductor.rpc.ConductorSparkApi;
import com.kryptnostic.conductor.rpc.odata.DatastoreConstants;
import com.kryptnostic.datastore.services.CassandraTableManager;
import com.kryptnostic.datastore.services.EdmManager;
import com.kryptnostic.datastore.services.EdmService;
import com.kryptnostic.rhizome.configuration.cassandra.CassandraConfiguration;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;
import com.kryptnostic.rhizome.pods.CassandraPod;
import com.kryptnostic.rhizome.registries.ObjectMapperRegistry;
import com.kryptnostic.sparks.ConductorSparkImpl;
import com.kryptnostic.sparks.SparkAuthorizationManager;

@Configuration
@Import( CassandraPod.class )
public class ConductorSparkPod {
    // TODO: Hack to avoid circular dependency... need to move Spark Jars config into rhizome.yaml
    ConductorConfiguration         conductorConfiguration = ConfigurationService.StaticLoader
            .loadConfiguration( ConductorConfiguration.class );
    String[]                       sparkMasters           = new String[] { "mjolnir:7077", "mjolnir.local:7077",
            "localhost:7077", "cassandra:7077"};

    @Inject
    private CassandraConfiguration cassandraConfiguration;

    @Inject
    private Session                session;

    @Inject
    private HazelcastInstance      hazelcastInstance;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return ObjectMapperRegistry.getJsonMapper();
    }

    @Bean
    public MappingManager mappingManager() {
        return new MappingManager( session );
    }

    @Bean
    public CassandraTableManager tableManager() {
        return new CassandraTableManager(
                hazelcastInstance,
                DatastoreConstants.KEYSPACE,
                session,
                mappingManager() );
    }

    @Bean
    public EdmManager dataModelService() {
        return new EdmService( session, mappingManager(), tableManager() );
    }

    @Bean
    public SparkConf sparkConf() {
        StringBuilder sparkMasterUrlBuilder = new StringBuilder( "spark://" );
        String sparkMastersAsString = Arrays.asList( sparkMasters ).stream().collect( Collectors.joining( "," ) );
        sparkMasterUrlBuilder.append( sparkMastersAsString );
        return new SparkConf().setAppName( "Kryptnostic Spark Conductor" )
                .setMaster( sparkMasterUrlBuilder.toString() )
                .setJars( conductorConfiguration.getSparkJars() )
                .set( "spark.cassandra.connection.host", cassandraConfiguration.getCassandraSeedNodes().stream()
                        .map( host -> host.getHostAddress() ).collect( Collectors.joining( "," ) ) )
                .set( "spark.cassandra.connection.port", Integer.toString( 9042 ) );
    }

    @Bean
    public JavaSparkContext javaSparkContext() {
        return new JavaSparkContext( sparkConf() );
    }

    @Bean
    public CassandraSQLContext cassandraSQLContext() {
        return new CassandraSQLContext( javaSparkContext().sc() );
    }

    @Bean
    public SparkContextJavaFunctions sparkContextJavaFunctions() {
        return CassandraJavaUtil.javaFunctions( javaSparkContext() );
    }

    @Bean
    public ConductorSparkApi api() {
        return new ConductorSparkImpl(
                DatastoreConstants.KEYSPACE,
                javaSparkContext(),
                cassandraSQLContext(),
                sparkContextJavaFunctions(),
                tableManager(),
                dataModelService(),
                new SparkAuthorizationManager() );
    }
}
