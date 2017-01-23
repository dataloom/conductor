package com.kryptnostic.conductor.pods;

import java.io.IOException;

import javax.inject.Inject;

import com.dataloom.mail.config.MailServiceRequirements;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dataloom.mappers.ObjectMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.rpc.ConductorConfiguration;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;

@Configuration
public class ConductorServicesPod {

    @Inject
    private HazelcastInstance    hazelcastInstance;

    @Inject
    private ConfigurationService configurationService;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return ObjectMappers.getJsonMapper();
    }

    @Bean
    public ConductorConfiguration getConductorConfiguration() throws IOException {
        return configurationService.getConfiguration( ConductorConfiguration.class );
    }


}
