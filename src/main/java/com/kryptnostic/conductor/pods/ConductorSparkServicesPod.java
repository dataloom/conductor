package com.kryptnostic.conductor.pods;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dataloom.mappers.ObjectMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;

@Configuration
public class ConductorSparkServicesPod {

    @Inject
    private HazelcastInstance    hazelcastInstance;

    @Inject
    private ConfigurationService configurationService;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return ObjectMappers.getJsonMapper();
    }
    
}
