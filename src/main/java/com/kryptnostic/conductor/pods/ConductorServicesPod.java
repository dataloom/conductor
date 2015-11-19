package com.kryptnostic.conductor.pods;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;
import com.kryptnostic.kodex.v1.serialization.jackson.KodexObjectMapperFactory;

public class ConductorServicesPod {

    @Inject
    private HazelcastInstance hazelcastInstance;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return KodexObjectMapperFactory.getObjectMapper();
    }

    @Bean
    public ServiceRegistrationService getServiceRegistrationService() {
        return new ServiceRegistrationService( hazelcastInstance );
    }

}
