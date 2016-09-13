package com.kryptnostic.conductor.pods;

import javax.inject.Inject;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.SparkContextJavaFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.codecs.FullQualifiedNameTypeCodec;
import com.kryptnostic.conductor.rpc.ConductorSparkApi;
import com.kryptnostic.conductor.rpc.odata.DatastoreConstants;
import com.kryptnostic.datastore.services.CassandraTableManager;
import com.kryptnostic.datastore.services.EdmManager;
import com.kryptnostic.datastore.services.EdmService;
import com.kryptnostic.rhizome.pods.SparkPod;
import com.kryptnostic.rhizome.registries.ObjectMapperRegistry;
import com.kryptnostic.sparks.ConductorSparkImpl;
import com.kryptnostic.sparks.SparkAuthorizationManager;

@Configuration
@Import( SparkPod.class )
public class ConductorSparkPod {

    @Inject
    private Session                session;

    @Inject
    private HazelcastInstance      hazelcastInstance;

    @Inject
    private JavaSparkContext  javaSparkContext;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return ObjectMapperRegistry.getJsonMapper();
    }

    @Bean
    public FullQualifiedNameTypeCodec fullQualifiedNameTypeCodec() {
        return new FullQualifiedNameTypeCodec();
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
    public SQLContext cassandraSQLContext() {
        return new SQLContext( javaSparkContext.sc() );
    }

    @Bean
    public SparkContextJavaFunctions sparkContextJavaFunctions() {
        return CassandraJavaUtil.javaFunctions( javaSparkContext );
    }

    @Bean
    public ConductorSparkApi api() {
        return new ConductorSparkImpl(
                DatastoreConstants.KEYSPACE,
                javaSparkContext,
                cassandraSQLContext(),
                sparkContextJavaFunctions(),
                tableManager(),
                dataModelService(),
                new SparkAuthorizationManager() );
    }
}
