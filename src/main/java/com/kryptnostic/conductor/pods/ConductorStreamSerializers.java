package com.kryptnostic.conductor.pods;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kryptnostic.conductor.ConductorCallStreamSerializer;
import com.kryptnostic.conductor.ConductorSparkApi;
import com.kryptnostic.conductor.EmployeeStreamSerializer;

@Configuration
public class ConductorStreamSerializers {
    @Inject
    private ConductorSparkApi api;

    @Bean
    public ConductorCallStreamSerializer ccss() {
        return new ConductorCallStreamSerializer( api );
    }

    @Bean
    public EmployeeStreamSerializer ess() {
        return new EmployeeStreamSerializer();
    }
}
