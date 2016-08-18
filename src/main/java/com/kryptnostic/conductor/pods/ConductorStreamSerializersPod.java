package com.kryptnostic.conductor.pods;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kryptnostic.conductor.rpc.ConductorSparkApi;
import com.kryptnostic.conductor.rpc.LambdaStreamSerializer;
import com.kryptnostic.conductor.rpc.serializers.ConductorCallStreamSerializer;
import com.kryptnostic.conductor.rpc.serializers.EmployeeStreamSerializer;

@Configuration
public class ConductorStreamSerializersPod {
    @Inject
    private ConductorSparkApi api;

    @Bean 
    public LambdaStreamSerializer lss() {
        return new LambdaStreamSerializer();
    }
    @Bean
    public ConductorCallStreamSerializer ccss() {
        return new ConductorCallStreamSerializer( api );
    }

    @Bean
    public EmployeeStreamSerializer ess() {
        return new EmployeeStreamSerializer();
    }
}
