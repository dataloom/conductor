package com.kryptnostic.conductor;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.annotation.Configuration;

import com.kryptnostic.conductor.rpc.ConductorSparkApi;
import com.kryptnostic.conductor.rpc.serializers.ConductorCallStreamSerializer;

@Configuration
public class PlasmaCoupling {
    @Inject
    private ConductorSparkApi             api;

    @Inject
    private ConductorCallStreamSerializer ccss;

    @PostConstruct
    public void connect() {
        ccss.setConductorSparkApi( api );
    }

}
