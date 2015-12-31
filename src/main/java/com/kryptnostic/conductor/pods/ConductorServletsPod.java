package com.kryptnostic.conductor.pods;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Lists;
import com.kryptnostic.rhizome.configuration.servlets.DispatcherServletConfiguration;

@Configuration
public class ConductorServletsPod {
    @Bean
    public DispatcherServletConfiguration restServlet() {
        return new DispatcherServletConfiguration(
                "conductor",
                new String[] { "/conductor/*" },
                1,
                Lists.<Class<?>> newArrayList( ConductorMvcPod.class ) );
    }

}
