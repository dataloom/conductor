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

import com.amazonaws.services.s3.AmazonS3;
import com.codahale.metrics.MetricRegistry;
import com.dataloom.mappers.ObjectMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.rhizome.configuration.ConfigurationConstants.Profiles;
import com.kryptnostic.rhizome.configuration.amazon.AmazonLaunchConfiguration;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;
import com.openlattice.ResourceConfigurationLoader;
import com.openlattice.assembler.Assembler;
import com.openlattice.assembler.Assembler.EntitySetViewsInitializerTask;
import com.openlattice.assembler.Assembler.OrganizationAssembliesInitializerTask;
import com.openlattice.assembler.AssemblerConfiguration;
import com.openlattice.assembler.AssemblerConnectionManager;
import com.openlattice.assembler.AssemblerDependencies;
import com.openlattice.assembler.pods.AssemblerConfigurationPod;
import com.openlattice.assembler.tasks.CleanOutOldUsersInitializationTask;
import com.openlattice.assembler.tasks.ProductionViewSchemaInitializationTask;
import com.openlattice.assembler.tasks.UsersAndRolesInitializationTask;
import com.openlattice.auditing.AuditInitializationTask;
import com.openlattice.auditing.AuditTaskDependencies;
import com.openlattice.auditing.AuditingConfiguration;
import com.openlattice.auditing.pods.AuditingConfigurationPod;
import com.openlattice.auth0.Auth0TokenProvider;
import com.openlattice.authentication.Auth0Configuration;
import com.openlattice.authorization.*;
import com.openlattice.authorization.initializers.AuthorizationInitializationDependencies;
import com.openlattice.authorization.initializers.AuthorizationInitializationTask;
import com.openlattice.conductor.rpc.ConductorConfiguration;
import com.openlattice.conductor.rpc.MapboxConfiguration;
import com.openlattice.data.EntityDatastore;
import com.openlattice.data.EntityKeyIdService;
import com.openlattice.data.ids.PostgresEntityKeyIdService;
import com.openlattice.data.storage.ByteBlobDataManager;
import com.openlattice.data.storage.HazelcastEntityDatastore;
import com.openlattice.data.storage.IndexingMetadataManager;
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
import com.openlattice.linking.PostgresLinkingFeedbackService;
import com.openlattice.linking.graph.PostgresLinkingQueryService;
import com.openlattice.mail.MailServiceClient;
import com.openlattice.mail.config.MailServiceRequirements;
import com.openlattice.organizations.HazelcastOrganizationService;
import com.openlattice.organizations.roles.HazelcastPrincipalService;
import com.openlattice.organizations.roles.SecurePrincipalsManager;
import com.openlattice.organizations.tasks.OrganizationMembersCleanupDependencies;
import com.openlattice.organizations.tasks.OrganizationMembersCleanupInitializationTask;
import com.openlattice.organizations.tasks.OrganizationsInitializationDependencies;
import com.openlattice.organizations.tasks.OrganizationsInitializationTask;
import com.openlattice.postgres.PostgresTableManager;
import com.openlattice.search.PersistentSearchMessenger;
import com.openlattice.search.PersistentSearchMessengerHelpers;
import com.openlattice.search.SearchService;
import com.openlattice.tasks.PostConstructInitializerTaskDependencies;
import com.openlattice.tasks.PostConstructInitializerTaskDependencies.PostConstructInitializerTask;
import com.openlattice.users.Auth0SyncInitializationTask;
import com.openlattice.users.Auth0SyncInitializationTaskDependencies;
import com.openlattice.users.Auth0SyncTask;
import com.openlattice.users.Auth0SyncTaskDependencies;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.openlattice.search.PersistentSearchMessengerKt.ALERT_MESSENGER_INTERVAL_MILLIS;

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
                eventBus );
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
    public PostConstructInitializerTaskDependencies postInitializerDependencies() {
        return new PostConstructInitializerTaskDependencies();
    }

    @Bean
    public PostConstructInitializerTask postInitializerTask() {
        return new PostConstructInitializerTask();
    }

    @Bean
    public Assembler assembler() {
        return new Assembler( authorizationManager(),
                dbcs(),
                hikariDataSource,
                metricRegistry,
                hazelcastInstance,
                eventBus );
    }

    @Bean
    public OrganizationsInitializationDependencies organizationBootstrapDependencies() {
        return new OrganizationsInitializationDependencies( organizationsManager() );
    }

    @Bean
    public OrganizationMembersCleanupDependencies organizationMembersCleanupDependencies() {
        return new OrganizationMembersCleanupDependencies( principalService(),
                organizationsManager(),
                securableObjectTypes() );
    }

    @Bean
    public OrganizationMembersCleanupInitializationTask organizationMembersCleanupInitializationTask() {
        return new OrganizationMembersCleanupInitializationTask();
    }

    @Bean
    public AuthorizationInitializationDependencies authorizationBootstrapDependencies() {
        return new AuthorizationInitializationDependencies( principalService() );
    }

    @Bean
    public AssemblerDependencies assemblerDependencies() {
        return new AssemblerDependencies(
                assemblerConfiguration,
                hikariDataSource,
                principalService(),
                organizationsManager(),
                dbcs(),
                hazelcastInstance.getMap( HazelcastMap.ENTITY_SETS.name() ),
                assemblerConnectionManager(),
                hazelcastInstance.getMap( HazelcastMap.SECURABLE_OBJECT_TYPES.name() ),
                metricRegistry );
    }

    @Bean
    public AuthorizationInitializationTask authorizationBootstrap() {
        return new AuthorizationInitializationTask();
    }

    @Bean
    public UsersAndRolesInitializationTask assemblerInitializationTask() {
        return new UsersAndRolesInitializationTask();
    }

    @Bean
    public ProductionViewSchemaInitializationTask productionViewSchemaInitializationTask() {
        return new ProductionViewSchemaInitializationTask();
    }

    @Bean
    public CleanOutOldUsersInitializationTask cleanOutOldUsersInitializationTask() {
        return new CleanOutOldUsersInitializationTask();
    }

    @Bean
    public OrganizationAssembliesInitializerTask organizationAssembliesInitializerTask() {
        return new OrganizationAssembliesInitializerTask();
    }

    @Bean
    public EntitySetViewsInitializerTask entityViewsInitializerTask() {
        return new EntitySetViewsInitializerTask();
    }

    @Bean
    public AuditTaskDependencies auditTaskDependencies() {
        return new AuditTaskDependencies(
                principalService(),
                dataModelService(),
                authorizationManager() );
    }

    @Bean
    public AssemblerConnectionManager assemblerConnectionManager() {
        return new AssemblerConnectionManager( assemblerConfiguration,
                hikariDataSource,
                principalService(),
                organizationsManager(),
                dbcs(),
                hazelcastInstance,
                eventBus,
                metricRegistry );
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
    public OrganizationsInitializationTask organizationBootstrap() {
        return new OrganizationsInitializationTask();
    }

    @Bean
    public Auth0TokenProvider auth0TokenProvider() {
        return new Auth0TokenProvider( auth0Configuration );
    }

    @Bean
    public Auth0SyncTaskDependencies auth0SyncTaskDependencies() {
        return new Auth0SyncTaskDependencies( hazelcastInstance,
                principalService(),
                organizationsManager(),
                dbcs(),
                auth0TokenProvider() );
    }

    @Bean
    public Auth0SyncTask auth0SyncTask() {
        return new Auth0SyncTask();
    }

    @Bean
    public Auth0SyncInitializationTaskDependencies auth0SyncInitializationTaskDependencies() {
        return new Auth0SyncInitializationTaskDependencies( auth0SyncTask() );
    }

    @Bean
    public Auth0SyncInitializationTask auth0SyncInitializationTask() {
        return new Auth0SyncInitializationTask();
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
    public SecurableObjectResolveTypeService securableObjectTypes() {
        return new HazelcastSecurableObjectResolveTypeService( hazelcastInstance );
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
    public IndexingMetadataManager postgresDataManager() {
        return new IndexingMetadataManager( hikariDataSource );
    }

    @Bean
    public EdmAuthorizationHelper authorizingComponent() {
        return new EdmAuthorizationHelper( dataModelService(), authorizationManager() );
    }

    @Bean
    public AuditInitializationTask auditInitializationTask() {
        return new AuditInitializationTask( hazelcastInstance );
    }

    @Bean
    public LinkingQueryService lqs() {
        return new PostgresLinkingQueryService( hikariDataSource );
    }

    @Bean
    public PostgresLinkingFeedbackService postgresLinkingFeedbackQueryService() {
        return new PostgresLinkingFeedbackService( hikariDataSource, hazelcastInstance );
    }
}
