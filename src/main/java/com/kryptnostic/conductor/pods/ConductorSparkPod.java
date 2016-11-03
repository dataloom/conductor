package com.kryptnostic.conductor.pods;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.extras.codecs.enums.EnumNameCodec;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.SparkContextJavaFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.codecs.EnumSetTypeCodec;
import com.kryptnostic.conductor.codecs.FullQualifiedNameTypeCodec;
import com.kryptnostic.conductor.rpc.ConductorSparkApi;
import com.kryptnostic.conductor.rpc.odata.DatastoreConstants;
import com.kryptnostic.conductor.rpc.serializers.ConductorCallStreamSerializer;
import com.kryptnostic.conductor.rpc.serializers.QueryResultStreamSerializer;
import com.kryptnostic.datastore.services.CassandraTableManager;
import com.kryptnostic.datastore.services.EdmManager;
import com.kryptnostic.datastore.services.EdmService;
import com.kryptnostic.datastore.services.PermissionsService;
import com.kryptnostic.datastore.Permission;
import com.kryptnostic.datastore.services.ActionAuthorizationService;
import com.kryptnostic.rhizome.pods.SparkPod;
import com.kryptnostic.rhizome.registries.ObjectMapperRegistry;
import com.kryptnostic.sparks.ConductorSparkImpl;
import com.kryptnostic.sparks.SparkAuthorizationManager;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Configuration
@Import( SparkPod.class )
public class ConductorSparkPod {

    @Inject
    private Session session;

    @Inject
    private QueryResultStreamSerializer qrss;

    @Inject
    private HazelcastInstance hazelcastInstance;

    @Inject
    private SparkSession sparkSession;

    @Inject
    private ConductorCallStreamSerializer ccss;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return ObjectMapperRegistry.getJsonMapper();
    }

    @Bean
    public TypeCodec<Set<String>> setStringCodec() {
        return TypeCodec.set( TypeCodec.varchar() );
    }
    
    @Bean
    public FullQualifiedNameTypeCodec fullQualifiedNameTypeCodec() {
        return new FullQualifiedNameTypeCodec();
    }

    @Bean
    public TypeCodec<EdmPrimitiveTypeKind> edmPrimitiveTypeKindTypeCodec() {
        return new EnumNameCodec<EdmPrimitiveTypeKind>( EdmPrimitiveTypeKind.class );
    }
    
    @Bean
    public EnumNameCodec<Permission> permissionCodec(){
        return new EnumNameCodec<>( Permission.class);
    }
    
    @Bean
    public TypeCodec<EnumSet<Permission>> enumSetPermissionCodec(){
        return new EnumSetTypeCodec<Permission>( permissionCodec() );
    }

    @Bean
    public MappingManager mappingManager() {
        return new MappingManager( session );
    }

    @Bean
    public CassandraTableManager tableManager() {
        return new CassandraTableManager(
                DatastoreConstants.KEYSPACE,
                session,
                mappingManager() );
    }

    @Bean
    public PermissionsService permissionsService() {
        return new PermissionsService( session, mappingManager(), tableManager() );
    }
    
    @Bean
    public ActionAuthorizationService authzService() {
        return new ActionAuthorizationService( permissionsService() );
    }
    
    @Bean
    public EdmManager dataModelService() {
        return new EdmService( session, mappingManager(), tableManager(), permissionsService() );
    }

    @Bean
    public SparkContextJavaFunctions sparkContextJavaFunctions() {
        return CassandraJavaUtil.javaFunctions( sparkSession.sparkContext() );
    }

    @Bean
    public ConductorSparkApi api() {
        ConductorSparkApi api =  new ConductorSparkImpl(
                DatastoreConstants.KEYSPACE,
                sparkSession,
                sparkContextJavaFunctions(),
                tableManager(),
                dataModelService(),
                new SparkAuthorizationManager() );
        ccss.setConductorSparkApi( api );
        return api;
    }

    @PostConstruct
    public void setSession() {
        qrss.setSession( session );
    }
}
