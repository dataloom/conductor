package com.kryptnostic.conductor.pods;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekbeast.rhizome.configuration.service.ConfigurationService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;
import com.kryptnostic.kodex.v1.serialization.jackson.KodexObjectMapperFactory;
import com.kryptnostic.rhizome.emails.EmailService;

public class ConductorServicesPod {

    @Inject
    private ConfigurationService config;

    @Inject
    private HazelcastInstance    hazelcastInstance;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return KodexObjectMapperFactory.getObjectMapper();
    }
    
    @Bean
    public ServiceRegistrationService getServiceRegistrationService() {
    	return new ServiceRegistrationService(hazelcastInstance);
    }
    
    @Bean
    public EmailService emailService() throws IOException {
    	return new EmailService(config);
    }
    
}
