package com.kryptnostic.conductor.pods;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geekbeast.rhizome.configuration.service.ConfigurationService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.ConductorConfiguration;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;

@Configuration
public class ConductorServicesPod {

    @Inject
    private HazelcastInstance    hazelcastInstance;

    @Inject
    private ConfigurationService config;

    @Bean
    public ServiceRegistrationService getServiceRegistrationService() {
        return new ServiceRegistrationService( hazelcastInstance );
    }

    @Bean
    public ConductorConfiguration reportEmailAddress() throws IOException {
        return config.getConfiguration( ConductorConfiguration.class );
    }

}
