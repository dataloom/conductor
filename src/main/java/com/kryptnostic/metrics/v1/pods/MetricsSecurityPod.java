package com.kryptnostic.metrics.v1.pods;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class MetricsSecurityPod extends WebSecurityConfigurerAdapter {

    public MetricsSecurityPod() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void configure( HttpSecurity http ) throws Exception {
        http.authorizeRequests().antMatchers( "/**" ).permitAll().and().csrf().disable();
    }
}
