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

import static com.google.common.base.Preconditions.checkState;
import static com.kryptnostic.datastore.util.Util.returnAndLog;

import com.amazonaws.services.s3.AmazonS3;
import com.dataloom.authorization.AuthorizationManager;
import com.dataloom.authorization.AuthorizationQueryService;
import com.dataloom.authorization.HazelcastAclKeyReservationService;
import com.dataloom.authorization.HazelcastAuthorizationService;
import com.dataloom.directory.UserDirectoryService;
import com.dataloom.mappers.ObjectMappers;
import com.dataloom.organizations.HazelcastOrganizationService;
import com.dataloom.organizations.roles.HazelcastPrincipalService;
import com.dataloom.organizations.roles.SecurePrincipalsManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.rpc.ConductorConfiguration;
import com.kryptnostic.rhizome.configuration.ConfigurationConstants.Profiles;
import com.kryptnostic.rhizome.configuration.amazon.AmazonLaunchConfiguration;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;
import com.openlattice.ResourceConfigurationLoader;
import com.openlattice.authorization.DbCredentialService;
import com.openlattice.bootstrap.AuthorizationBootstrap;
import com.openlattice.bootstrap.OrganizationBootstrap;
import com.openlattice.conductor.users.Auth0Synchronizer;
import com.openlattice.conductor.users.Auth0Synchronizer.Auth0SyncDriver;
import com.zaxxer.hikari.HikariDataSource;
import digital.loom.rhizome.configuration.auth0.Auth0Configuration;
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

    @Inject
    private Auth0Configuration auth0Configuration;

    @Inject
    private HikariDataSource hikariDataSource;

    @Inject
    private EventBus eventBus;

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
        ConductorConfiguration config = configurationService.getConfiguration( ConductorConfiguration.class );
        logger.info( "Using local conductor configuration: {}", config );
        return config;
    }

    @Bean( name = "conductorConfiguration" )
    @Profile( Profiles.AWS_CONFIGURATION_PROFILE )
    public ConductorConfiguration getAwsConductorConfiguration() throws IOException {

        ConductorConfiguration config = ResourceConfigurationLoader.loadConfigurationFromS3( s3,
                awsLaunchConfig.getBucket(),
                awsLaunchConfig.getFolder(),
                ConductorConfiguration.class );

        logger.info( "Using aws conductor configuration: {}", config );
        return config;
    }

    @Bean
    public DbCredentialService dbcs() {
        return new DbCredentialService( hazelcastInstance, hikariDataSource );
    }

    @Bean
    public AuthorizationQueryService authorizationQueryService() {
        return new AuthorizationQueryService( hikariDataSource, hazelcastInstance );
    }

    @Bean
    public HazelcastAclKeyReservationService aclKeyReservationService() {
        return new HazelcastAclKeyReservationService( hazelcastInstance );
    }

    @Bean
    public SecurePrincipalsManager principalService() {
        return new HazelcastPrincipalService( hazelcastInstance,
                aclKeyReservationService(),
                authorizationManager() );
    }

    @Bean
    public AuthorizationManager authorizationManager() {
        return new HazelcastAuthorizationService( hazelcastInstance, authorizationQueryService(), eventBus );
    }

    @Bean
    public UserDirectoryService userDirectoryService() {
        return new UserDirectoryService( auth0Configuration.getToken(), hazelcastInstance );
    }

    @Bean
    public HazelcastOrganizationService organizationsManager() {
        return new HazelcastOrganizationService(
                hazelcastInstance,
                aclKeyReservationService(),
                authorizationManager(),
                userDirectoryService(),
                principalService() );
    }

    @Bean
    public AuthorizationBootstrap authzBoot() {
        return returnAndLog( new AuthorizationBootstrap( hazelcastInstance, principalService() ),
                "Checkpoint AuthZ Boostrap" );
    }

    @Bean
    public OrganizationBootstrap orgBoot() {
        checkState( authzBoot().isInitialized(), "Roles must be initialized." );
        return returnAndLog( new OrganizationBootstrap( organizationsManager() ),
                "Checkpoint organization bootstrap." );
    }

    @Bean
    public Auth0Synchronizer auth0Refresher() {
        return new Auth0Synchronizer( hazelcastInstance, principalService(), dbcs(), auth0Configuration.getToken() );
    }

    @Bean
    public Auth0SyncDriver auth0RefreshDriver() {
        return new Auth0SyncDriver( auth0Refresher() );
    }

}
