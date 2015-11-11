package com.kryptnostic.metrics.v1.pods;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geekbeast.rhizome.configuration.servlets.DispatcherServletConfiguration;
import com.google.common.collect.Lists;

@Configuration
public class MetricsServletsPod {
    @Bean
    public DispatcherServletConfiguration restServlet() {
        return new DispatcherServletConfiguration(
                "app",
                new String[] { "/v1/*" },
                1,
                Lists.<Class<?>> newArrayList( MetricsMvcPod.class ) );
    }
}
