/*
 * Copyright (C) 2018. OpenLattice, Inc.
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
 * You can contact the owner of the copyright at support@openlattice.com
 *
 */

package com.openlattice.pods;

import static com.google.common.base.Preconditions.checkState;
import static com.openlattice.datastore.util.Util.returnAndLog;
import static com.openlattice.search.PersistentSearchMessengerKt.ALERT_MESSENGER_INTERVAL_MILLIS;
import static com.openlattice.users.Auth0SyncTaskKt.REFRESH_INTERVAL_MILLIS;

import com.amazonaws.services.s3.AmazonS3;
import com.codahale.metrics.MetricRegistry;
import com.dataloom.mappers.ObjectMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.rhizome.configuration.ConfigurationConstants.Profiles;
import com.kryptnostic.rhizome.configuration.amazon.AmazonLaunchConfiguration;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;
import com.openlattice.ResourceConfigurationLoader;
import com.openlattice.assembler.Assembler;
import com.openlattice.assembler.AssemblerConfiguration;
import com.openlattice.assembler.AssemblerConnectionManager;
import com.openlattice.assembler.pods.AssemblerConfigurationPod;
import com.openlattice.auditing.AuditingConfiguration;
import com.openlattice.auditing.pods.AuditingConfigurationPod;
import com.openlattice.auth0.Auth0TokenProvider;
import com.openlattice.authentication.Auth0Configuration;
import com.openlattice.authorization.AbstractSecurableObjectResolveTypeService;
import com.openlattice.authorization.AuthorizationManager;
import com.openlattice.authorization.AuthorizationQueryService;
import com.openlattice.authorization.DbCredentialService;
import com.openlattice.authorization.EdmAuthorizationHelper;
import com.openlattice.authorization.HazelcastAbstractSecurableObjectResolveTypeService;
import com.openlattice.authorization.HazelcastAclKeyReservationService;
import com.openlattice.authorization.HazelcastAuthorizationService;
import com.openlattice.authorization.PostgresUserApi;
import com.openlattice.bootstrap.AuthorizationBootstrap;
import com.openlattice.conductor.rpc.ConductorConfiguration;
import com.openlattice.conductor.rpc.MapboxConfiguration;
import com.openlattice.data.EntityDatastore;
import com.openlattice.data.EntityKeyIdService;
import com.openlattice.data.ids.PostgresEntityKeyIdService;
import com.openlattice.data.storage.ByteBlobDataManager;
import com.openlattice.data.storage.HazelcastEntityDatastore;
import com.openlattice.data.storage.PostgresDataManager;
import com.openlattice.data.storage.PostgresEntityDataQueryService;
import com.openlattice.datastore.pods.ByteBlobServicePod;
import com.openlattice.datastore.services.EdmManager;
import com.openlattice.datastore.services.EdmService;
import com.openlattice.directory.UserDirectoryService;
import com.openlattice.edm.PostgresEdmManager;
import com.openlattice.edm.properties.PostgresTypeManager;
import com.openlattice.edm.schemas.SchemaQueryService;
import com.openlattice.edm.schemas.manager.HazelcastSchemaManager;
import com.openlattice.edm.schemas.postgres.PostgresSchemaQueryService;
import com.openlattice.graph.Graph;
import com.openlattice.graph.core.GraphService;
import com.openlattice.hazelcast.HazelcastMap;
import com.openlattice.hazelcast.HazelcastQueue;
import com.openlattice.ids.HazelcastIdGenerationService;
import com.openlattice.linking.LinkingQueryService;
import com.openlattice.linking.graph.PostgresLinkingQueryService;
import com.openlattice.mail.MailServiceClient;
import com.openlattice.mail.config.MailServiceRequirements;
import com.openlattice.organizations.HazelcastOrganizationService;
import com.openlattice.organizations.OrganizationBootstrap;
import com.openlattice.organizations.roles.HazelcastPrincipalService;
import com.openlattice.organizations.roles.SecurePrincipalsManager;
import com.openlattice.postgres.PostgresTableManager;
import com.openlattice.search.PersistentSearchMessenger;
import com.openlattice.search.PersistentSearchMessengerHelpers;
import com.openlattice.search.SearchService;
import com.openlattice.users.Auth0SyncHelpers;
import com.openlattice.users.Auth0SyncTask;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Import( { ByteBlobServicePod.class, AuditingConfigurationPod.class, AssemblerConfigurationPod.class } )
public class ConductorServicesPod {
    private static Logger logger = LoggerFactory.getLogger( ConductorServicesPod.class );

    @Inject
    private PostgresTableManager tableManager;

    @Inject
    private HazelcastInstance hazelcastInstance;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private Auth0Configuration auth0Configuration;

    @Inject
    private AuditingConfiguration auditingConfiguration;

    @Inject
    private HikariDataSource hikariDataSource;

    @Inject
    private ByteBlobDataManager byteBlobDataManager;

    @Inject
    private PostgresUserApi pgUserApi;

    @Inject
    private EventBus eventBus;

    @Inject
    private ListeningExecutorService executor;

    @Inject
    private AssemblerConfiguration assemblerConfiguration;

    @Autowired( required = false )
    private AmazonS3 s3;

    @Autowired( required = false )
    private AmazonLaunchConfiguration awsLaunchConfig;

    @Inject
    private MetricRegistry metricRegistry;

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
    @Profile( { Profiles.AWS_CONFIGURATION_PROFILE, Profiles.AWS_TESTING_PROFILE } )
    public ConductorConfiguration getAwsConductorConfiguration() throws IOException {
        ConductorConfiguration config = ResourceConfigurationLoader.loadConfigurationFromS3( s3,
                awsLaunchConfig.getBucket(),
                awsLaunchConfig.getFolder(),
                ConductorConfiguration.class );

        logger.info( "Using aws conductor configuration: {}", config );
        return config;
    }

    @Bean( name = "mapboxConfiguration" )
    public MapboxConfiguration mapboxConfiguration() throws IOException {
        return configurationService.getConfiguration( MapboxConfiguration.class );
    }

    @Bean
    public DbCredentialService dbcs() {
        return new DbCredentialService( hazelcastInstance, pgUserApi );
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
                authorizationManager(),
                assembler() );
    }

    @Bean
    public AuthorizationManager authorizationManager() {
        return new HazelcastAuthorizationService( hazelcastInstance, authorizationQueryService(), eventBus );
    }

    @Bean
    public UserDirectoryService userDirectoryService() {
        return new UserDirectoryService( auth0TokenProvider(), hazelcastInstance );
    }

    @Bean
    public Assembler assembler() {
        return new Assembler( assemblerConfiguration,
                authorizationManager(),
                dbcs(),
                hikariDataSource,
                hazelcastInstance );
    }

    @Bean
    public AssemblerConnectionManager bootstrapRolesAndUsers() {
        final var hos = organizationsManager();

        AssemblerConnectionManager.initializeMetrics( metricRegistry );
        AssemblerConnectionManager.initializeAssemblerConfiguration( assemblerConfiguration );
        AssemblerConnectionManager.initializeProductionDatasource( hikariDataSource );
        AssemblerConnectionManager.initializeSecurePrincipalsManager( principalService() );
        AssemblerConnectionManager.initializeOrganizations( hos );
        AssemblerConnectionManager.initializeDbCredentialService( dbcs() );
        AssemblerConnectionManager.initializeEntitySets( hazelcastInstance.getMap( HazelcastMap.ENTITY_SETS.name() ) );
        AssemblerConnectionManager.initializeUsersAndRoles();

        if ( assemblerConfiguration.getInitialize().orElse( false ) ) {
            final var es = dataModelService().getEntitySet( assemblerConfiguration.getTestEntitySet().get() );
            final var org = hos.getOrganization( es.getOrganizationId() );
            final var apt = dataModelService()
                    .getPropertyTypesAsMap( dataModelService().getEntityType( es.getEntityTypeId() ).getProperties() );
            AssemblerConnectionManager.createOrganizationDatabase( org.getId() );
            final var results = AssemblerConnectionManager
                    .materializeEntitySets( org.getId(), ImmutableMap.of( es.getId(), apt ) );
            logger.info( "Results of materializing: {}", results );
        }
        return new AssemblerConnectionManager();
    }

    @Bean
    public HazelcastOrganizationService organizationsManager() {
        return new HazelcastOrganizationService(
                hazelcastInstance,
                aclKeyReservationService(),
                authorizationManager(),
                userDirectoryService(),
                principalService(),
                assembler() );
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
    public Auth0TokenProvider auth0TokenProvider() {
        return new Auth0TokenProvider( auth0Configuration );
    }

    @Bean
    public Auth0SyncTask auth0SyncTask() {
        var syncsExecutor = hazelcastInstance.getScheduledExecutorService( "syncs" );
        Auth0SyncHelpers.setHazelcastInstance( hazelcastInstance );
        Auth0SyncHelpers.setSpm( principalService() );
        Auth0SyncHelpers.setOrganizationService( organizationsManager() );
        Auth0SyncHelpers.setAuth0TokenProvider( auth0TokenProvider() );
        Auth0SyncHelpers.setDbCredentialService( dbcs() );
        Auth0SyncHelpers.setInitialized( true );
        final var taskCount = hazelcastInstance.getAtomicLong( "AUTH0_SYNC_TASK_COUNT" );
        final var syncTask = new Auth0SyncTask();
        if ( taskCount.incrementAndGet() == 1 ) {
            logger.info( "Scheduling auth0 sync task." );
            Auth0SyncHelpers.setSyncFuture(
                    syncsExecutor
                            .scheduleAtFixedRate( syncTask,
                                    0,
                                    REFRESH_INTERVAL_MILLIS,
                                    TimeUnit.MILLISECONDS ) );
        }
        return syncTask;
    }

    @Bean
    public MailServiceClient mailServiceClient() {
        return new MailServiceClient( mailServiceRequirements().getEmailQueue() );
    }

    @Bean
    public PersistentSearchMessenger persistentSearchMessenger() throws IOException {
        var alertMessengerExecutor = hazelcastInstance.getScheduledExecutorService( "alertMessenger" );
        PersistentSearchMessengerHelpers.setHds( hikariDataSource );
        PersistentSearchMessengerHelpers.setHazelcastInstance( hazelcastInstance );
        PersistentSearchMessengerHelpers.setPrincipalsManager( principalService() );
        PersistentSearchMessengerHelpers.setAuthorizationManager( authorizationManager() );
        PersistentSearchMessengerHelpers.setSearchService( searchService() );
        PersistentSearchMessengerHelpers.setMailServiceClient( mailServiceClient() );
        PersistentSearchMessengerHelpers.setMapboxToken( mapboxConfiguration().getMapboxToken() );
        PersistentSearchMessengerHelpers.setInitialized( true );

        final var taskCount = hazelcastInstance.getAtomicLong( "ALERT_MESSENGER_TASK_COUNT" );
        final var messengerTask = new PersistentSearchMessenger();
        if ( taskCount.incrementAndGet() == 1 ) {
            logger.info( "Scheduling alert messenger task." );
            PersistentSearchMessengerHelpers.setSyncFuture( alertMessengerExecutor
                    .scheduleAtFixedRate( messengerTask, 0, ALERT_MESSENGER_INTERVAL_MILLIS, TimeUnit.MILLISECONDS ) );
        }
        return messengerTask;
    }

    @Bean
    public SearchService searchService() {
        return new SearchService( eventBus );
    }

    @Bean
    public AbstractSecurableObjectResolveTypeService securableObjectTypes() {
        return new HazelcastAbstractSecurableObjectResolveTypeService( hazelcastInstance );
    }

    @Bean
    public SchemaQueryService schemaQueryService() {
        return new PostgresSchemaQueryService( hikariDataSource );
    }

    @Bean
    public PostgresEdmManager edmManager() {
        return new PostgresEdmManager( hikariDataSource, tableManager, hazelcastInstance );
    }

    @Bean
    public HazelcastSchemaManager schemaManager() {
        return new HazelcastSchemaManager( hazelcastInstance, schemaQueryService() );
    }

    @Bean
    public PostgresTypeManager entityTypeManager() {
        return new PostgresTypeManager( hikariDataSource );
    }

    @Bean
    public MailServiceRequirements mailServiceRequirements() {
        return () -> hazelcastInstance.getQueue( HazelcastQueue.EMAIL_SPOOL.name() );
    }

    @Bean
    public PostgresEntityDataQueryService dataQueryService() {
        return new PostgresEntityDataQueryService( hikariDataSource, byteBlobDataManager );
    }

    @Bean
    public EdmManager dataModelService() {
        return new EdmService(
                hikariDataSource,
                hazelcastInstance,
                aclKeyReservationService(),
                authorizationManager(),
                edmManager(),
                entityTypeManager(),
                schemaManager(),
                auditingConfiguration );
    }

    @Bean
    public GraphService graphService() {
        return new Graph( hikariDataSource, dataModelService() );
    }

    @Bean
    public EntityDatastore entityDatastore() {
        return new HazelcastEntityDatastore( idService(),
                postgresDataManager(),
                dataQueryService(),
                dataModelService() );
    }

    @Bean
    public HazelcastIdGenerationService idGenerationService() {
        return new HazelcastIdGenerationService( hazelcastInstance );
    }

    @Bean
    public EntityKeyIdService idService() {
        return new PostgresEntityKeyIdService( hazelcastInstance, hikariDataSource, idGenerationService() );
    }

    @Bean
    public PostgresDataManager postgresDataManager() {
        return new PostgresDataManager( hikariDataSource );
    }

    @Bean
    public EdmAuthorizationHelper authorizingComponent() {
        return new EdmAuthorizationHelper( dataModelService(), authorizationManager() );
    }

    @Bean
    public LinkingQueryService lqs() {
        return new PostgresLinkingQueryService( hikariDataSource );
    }
}
