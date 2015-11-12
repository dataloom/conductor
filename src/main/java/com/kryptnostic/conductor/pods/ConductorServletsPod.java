package com.kryptnostic.conductor.pods;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geekbeast.rhizome.configuration.servlets.DispatcherServletConfiguration;
import com.google.common.collect.Lists;

@Configuration
public class ConductorServletsPod {
    @Bean
    public DispatcherServletConfiguration restServlet() {
        return new DispatcherServletConfiguration(
                "app",
                new String[] { "/conductor/*" },
                1,
                Lists.<Class<?>> newArrayList( ConductorMvcPod.class ) );
    }
}
