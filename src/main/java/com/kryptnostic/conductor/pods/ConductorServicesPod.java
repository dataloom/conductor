package com.kryptnostic.conductor.pods;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekbeast.rhizome.configuration.service.ConfigurationService;
import com.geekbeast.rhizome.registries.ObjectMapperRegistry;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.ConductorConfiguration;
import com.kryptnostic.conductor.orchestra.OrchestraPackage;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;

@Configuration
@ComponentScan(
    basePackageClasses = { OrchestraPackage.class },
    includeFilters = @ComponentScan.Filter(
        value = {
                org.springframework.stereotype.Service.class },
        type = FilterType.ANNOTATION ) )
public class ConductorServicesPod {

    @Inject
    private HazelcastInstance    hazelcastInstance;

    @Inject
    private ConfigurationService configurationService;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return ObjectMapperRegistry.getPlainMapper();
    }

    @Bean
    public ServiceRegistrationService getServiceRegistrationService() {
        return new ServiceRegistrationService( hazelcastInstance );
    }

    @Bean
    public ConductorConfiguration getConductorConfiguration() throws IOException {
        return configurationService.getConfiguration( ConductorConfiguration.class );
    }

    // Blow is the second choice for adding beans to spring
    // @Bean
    // public MonitoringService monitoringService() throws IOException {
    // return new MonitoringService( hazelcastInstance, getConductorConfiguration() );
    // }
}
