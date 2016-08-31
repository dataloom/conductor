package com.kryptnostic.conductor.pods;

import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.SparkContextJavaFunctions;
import com.kryptnostic.rhizome.configuration.cassandra.CassandraConfiguration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.cassandra.CassandraSQLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kryptnostic.conductor.rpc.ConductorConfiguration;
import com.kryptnostic.conductor.rpc.ConductorSparkApi;
import com.kryptnostic.conductor.rpc.odata.DatastoreConstants;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;
import com.kryptnostic.sparks.ConductorSparkImpl;
import com.kryptnostic.sparks.SparkAuthorizationManager;

import javax.inject.Inject;

@Configuration
public class ConductorSparkPod {
    // TODO: Hack to avoid circular dependency... need to move Spark Jars config into rhizome.yaml
    ConductorConfiguration conductorConfiguration = ConfigurationService.StaticLoader
            .loadConfiguration(ConductorConfiguration.class);
    String[] sparkMasters = new String[]{"mjolnir:7077", "mjolnir.local:7077",
            "localhost:7077"};

    @Inject
    private CassandraConfiguration cassandraConfiguration;

    @Bean
    public SparkConf sparkConf() {
        StringBuilder sparkMasterUrlBuilder = new StringBuilder("spark://");
        for (int i = 0; i < sparkMasters.length; ) {
            sparkMasterUrlBuilder.append(sparkMasters[i]).append(i++ == sparkMasters.length ? "" : ",");
        }

        return new SparkConf().setAppName("Kryptnostic Spark Conductor")
                .setMaster(sparkMasterUrlBuilder.toString())
                .setJars(conductorConfiguration.getSparkJars())
                .set("spark.cassdra.connection.host", cassandraConfiguration.getCassandraSeedNodes().iterator().next().getHostAddress() );
    }

    @Bean
    public JavaSparkContext javaSparkContext() {
        return new JavaSparkContext(sparkConf());
    }

    @Bean
    public CassandraSQLContext cassandraSQLContext() {
        return new CassandraSQLContext(javaSparkContext().sc());
    }

    @Bean
    public SparkContextJavaFunctions sparkContextJavaFunctions() {
        return CassandraJavaUtil.javaFunctions(javaSparkContext());
    }

    @Bean
    public ConductorSparkApi api() {
        return new ConductorSparkImpl(
                DatastoreConstants.KEYSPACE,
                javaSparkContext(),
                cassandraSQLContext(),
                sparkContextJavaFunctions(),
                new SparkAuthorizationManager());
    }
}
