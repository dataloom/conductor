package com.kryptnostic.conductor.pods;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kryptnostic.conductor.rpc.ConductorConfiguration;
import com.kryptnostic.conductor.rpc.ConductorSparkApi;
import com.kryptnostic.conductor.rpc.odata.DatastoreConstants;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;
import com.kryptnostic.sparks.ConductorSparkImpl;
import com.kryptnostic.sparks.SparkAuthorizationManager;

@Configuration
public class ConductorSparkPod {
    // TODO: Hack to avoid circular dependency... need to move Spark Jars config into rhizome.yaml
    ConductorConfiguration conductorConfiguration = ConfigurationService.StaticLoader
            .loadConfiguration( ConductorConfiguration.class );
    String[]               sparkMasters           = new String[] { "mjolnir:7077", "mjolnir.local:7077",
            "localhost:7077" };

    @Bean
    public SparkConf sparkConf() {
        StringBuilder sparkMasterUrlBuilder = new StringBuilder( "spark://" );
        for ( int i = 0; i < sparkMasters.length; ) {
            sparkMasterUrlBuilder.append( sparkMasters[ i ] ).append( i++ == sparkMasters.length ? "" : "," );
        }

        return new SparkConf().setAppName( "Kryptnostic Spark Conductor" )
                .setMaster( sparkMasterUrlBuilder.toString() )
                .setJars( conductorConfiguration.getSparkJars() );
    }

    @Bean
    public JavaSparkContext javaSparkContext() {
        return new JavaSparkContext( sparkConf() );
    }

    @Bean
    public ConductorSparkApi api() {
        return new ConductorSparkImpl(
                DatastoreConstants.KEYSPACE,
                javaSparkContext(),
                new SparkAuthorizationManager() );
    }
}
