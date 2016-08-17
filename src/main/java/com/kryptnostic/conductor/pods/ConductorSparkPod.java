package com.kryptnostic.conductor.pods;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kryptnostic.conductor.ConductorSparkApi;
import com.kryptnostic.conductor.ConductorSparkImpl;

@Configuration
public class ConductorSparkPod {
    @Bean
    public SparkConf sparkConf() {
        return new SparkConf().setAppName( "Kryptnostic Spark Datastore" )
                .setMaster( "local" );
    }
    
    @Bean
    public JavaSparkContext javaSparkContext() {
        return new JavaSparkContext( sparkConf() );
    }
    
    @Bean
    public ConductorSparkApi api() {
        return new ConductorSparkImpl( javaSparkContext() );
    }
}
