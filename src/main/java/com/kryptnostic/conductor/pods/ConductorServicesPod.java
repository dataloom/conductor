/*
 * Copyright (C) 2017. Kryptnostic, Inc (dba Loom)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the owner of the copyright at support@thedataloom.com
 */

package com.kryptnostic.conductor.pods;

import com.amazonaws.services.s3.AmazonS3;
import com.dataloom.mappers.ObjectMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.rpc.ConductorConfiguration;
import com.kryptnostic.rhizome.configuration.ConfigurationConstants.Profiles;
import com.kryptnostic.rhizome.configuration.amazon.AmazonLaunchConfiguration;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;
import com.openlattice.ResourceConfigurationLoader;
import java.io.IOException;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ConductorServicesPod {
    private static Logger logger = LoggerFactory.getLogger( ConductorServicesPod.class );

    @Inject
    private HazelcastInstance hazelcastInstance;

    @Inject
    private ConfigurationService configurationService;

    @Autowired( required = false )
    private AmazonS3 s3;

    @Autowired( required = false )
    private AmazonLaunchConfiguration awsLaunchConfig;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return ObjectMappers.getJsonMapper();
    }

    @Bean( name = "conductorConfiguration" )
    @Profile( Profiles.LOCAL_CONFIGURATION_PROFILE )
    public ConductorConfiguration getLocalConductorConfiguration() throws IOException {
        ConductorConfiguration config =  configurationService.getConfiguration( ConductorConfiguration.class );
        logger.info("Using local conductor configuration: {}", config );
        return config;
    }

    @Bean( name = "conductorConfiguration" )
    @Profile( Profiles.AWS_CONFIGURATION_PROFILE )
    public ConductorConfiguration getAwsConductorConfiguration() throws IOException {

        ConductorConfiguration config =   ResourceConfigurationLoader.loadConfigurationFromS3( s3,
                awsLaunchConfig.getBucket(),
                awsLaunchConfig.getFolder(),
                ConductorConfiguration.class );

        logger.info("Using aws conductor configuration: {}", config );
        return config;
    }

}
