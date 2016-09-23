package com.kryptnostic.conductor.pods;

import com.kryptnostic.conductor.rpc.serializers.QueryResultStreamSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kryptnostic.conductor.rpc.LambdaStreamSerializer;
import com.kryptnostic.conductor.rpc.serializers.ConductorCallStreamSerializer;

@Configuration
public class ConductorStreamSerializersPod {
    @Bean
    public LambdaStreamSerializer lss() {
        return new LambdaStreamSerializer();
    }

    @Bean
    public ConductorCallStreamSerializer ccss() {
        return new ConductorCallStreamSerializer( null );
    }

    @Bean
    public QueryResultStreamSerializer qrss() {
        return new QueryResultStreamSerializer( null );
    }
}
