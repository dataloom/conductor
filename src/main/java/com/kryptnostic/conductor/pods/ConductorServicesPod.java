package com.kryptnostic.conductor.pods;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekbeast.rhizome.configuration.service.ConfigurationService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.kodex.v1.serialization.jackson.KodexObjectMapperFactory;

public class ConductorServicesPod {

    @Inject
    private ConfigurationService config;

    @Inject
    private HazelcastInstance    hazelcastInstance;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return KodexObjectMapperFactory.getObjectMapper();
    }
}
