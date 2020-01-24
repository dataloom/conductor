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
import com.auth0.client.mgmt.ManagementAPI;
import com.codahale.metrics.MetricRegistry;
import com.dataloom.mappers.ObjectMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekbeast.hazelcast.HazelcastClientProvider;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.rhizome.configuration.ConfigurationConstants.Profiles;
import com.kryptnostic.rhizome.configuration.amazon.AmazonLaunchConfiguration;
import com.kryptnostic.rhizome.configuration.service.ConfigurationService;
import com.openlattice.ResourceConfigurationLoader;
import com.openlattice.assembler.*;
import com.openlattice.assembler.Assembler.EntitySetViewsInitializerTask;
import com.openlattice.assembler.Assembler.OrganizationAssembliesInitializerTask;
import com.openlattice.assembler.pods.AssemblerConfigurationPod;
import com.openlattice.assembler.tasks.MaterializePermissionSyncTask;
import com.openlattice.assembler.tasks.MaterializedEntitySetsDataRefreshTask;
import com.openlattice.assembler.tasks.ProductionViewSchemaInitializationTask;
import com.openlattice.assembler.tasks.UsersAndRolesInitializationTask;
import com.openlattice.auditing.AuditInitializationTask;
import com.openlattice.auditing.AuditTaskDependencies;
import com.openlattice.auditing.AuditingConfiguration;
import com.openlattice.auditing.pods.AuditingConfigurationPod;
import com.openlattice.auth0.Auth0TokenProvider;
import com.openlattice.auth0.AwsAuth0TokenProvider;
import com.openlattice.authentication.Auth0Configuration;
import com.openlattice.authorization.*;
import com.openlattice.authorization.initializers.AuthorizationInitializationDependencies;
import com.openlattice.authorization.initializers.AuthorizationInitializationTask;
import com.openlattice.authorization.mapstores.ResolvedPrincipalTreesMapLoader;
import com.openlattice.authorization.mapstores.SecurablePrincipalsMapLoader;
import com.openlattice.conductor.rpc.ConductorConfiguration;
import com.openlattice.conductor.rpc.MapboxConfiguration;
import com.openlattice.data.EntityKeyIdService;
import com.openlattice.data.ids.PostgresEntityKeyIdService;
import com.openlattice.data.storage.*;
import com.openlattice.data.storage.partitions.PartitionManager;
import com.openlattice.datastore.pods.ByteBlobServicePod;
import com.openlattice.datastore.services.EdmManager;
import com.openlattice.datastore.services.EdmService;
import com.openlattice.datastore.services.EntitySetManager;
import com.openlattice.datastore.services.EntitySetService;
import com.openlattice.directory.Auth0UserDirectoryService;
import com.openlattice.directory.LocalUserDirectoryService;
import com.openlattice.directory.UserDirectoryService;
import com.openlattice.edm.PostgresEdmManager;
import com.openlattice.edm.properties.PostgresTypeManager;
import com.openlattice.edm.schemas.SchemaQueryService;
import com.openlattice.edm.schemas.manager.HazelcastSchemaManager;
import com.openlattice.edm.schemas.postgres.PostgresSchemaQueryService;
import com.openlattice.graph.Graph;
import com.openlattice.graph.GraphQueryService;
import com.openlattice.graph.PostgresGraphQueryService;
import com.openlattice.graph.core.GraphService;
import com.openlattice.hazelcast.HazelcastClient;
import com.openlattice.hazelcast.HazelcastMap;
import com.openlattice.hazelcast.HazelcastQueue;
import com.openlattice.ids.HazelcastIdGenerationService;
import com.openlattice.ids.tasks.IdGenerationCatchUpTask;
import com.openlattice.ids.tasks.IdGenerationCatchupDependency;
import com.openlattice.linking.LinkingQueryService;
import com.openlattice.linking.PostgresLinkingFeedbackService;
import com.openlattice.linking.graph.PostgresLinkingQueryService;
import com.openlattice.mail.MailServiceClient;
import com.openlattice.mail.config.MailServiceRequirements;
import com.openlattice.notifications.sms.PhoneNumberService;
import com.openlattice.organizations.HazelcastOrganizationService;
import com.openlattice.organizations.roles.HazelcastPrincipalService;
import com.openlattice.organizations.roles.SecurePrincipalsManager;
import com.openlattice.organizations.tasks.OrganizationMembersCleanupDependencies;
import com.openlattice.organizations.tasks.OrganizationMembersCleanupInitializationTask;
import com.openlattice.organizations.tasks.OrganizationsInitializationDependencies;
import com.openlattice.organizations.tasks.OrganizationsInitializationTask;
import com.openlattice.postgres.PostgresTableManager;
import com.openlattice.postgres.tasks.PostgresMetaDataPropertiesInitializationDependency;
import com.openlattice.postgres.tasks.PostgresMetaDataPropertiesInitializationTask;
import com.openlattice.search.SearchService;
import com.openlattice.subscriptions.PostgresSubscriptionService;
import com.openlattice.subscriptions.SubscriptionNotificationDependencies;
import com.openlattice.subscriptions.SubscriptionNotificationTask;
import com.openlattice.subscriptions.SubscriptionService;
import com.openlattice.tasks.PostConstructInitializerTaskDependencies;
import com.openlattice.tasks.PostConstructInitializerTaskDependencies.PostConstructInitializerTask;
import com.openlattice.users.Auth0SyncInitializationTask;
import com.openlattice.users.Auth0SyncService;
import com.openlattice.users.Auth0SyncTask;
import com.openlattice.users.Auth0SyncTaskDependencies;
import com.openlattice.users.Auth0UserListingService;
import com.openlattice.users.LocalUserListingService;
import com.openlattice.users.UserListingService;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;

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

    @Inject
    private HazelcastClientProvider hazelcastClientProvider;

    @Inject
    private SecurablePrincipalsMapLoader spml;

    @Inject
    private ResolvedPrincipalTreesMapLoader rptml;

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
    @Profile( Profiles.LOCAL_CONFIGURATION_PROFILE )
    public MapboxConfiguration getLocalMapboxConfiguration() throws IOException {
        return configurationService.getConfiguration( MapboxConfiguration.class );
    }

    @Bean( name = "mapboxConfiguration" )
    @Profile( { Profiles.AWS_CONFIGURATION_PROFILE, Profiles.AWS_TESTING_PROFILE } )
    public MapboxConfiguration getAwsMapboxConfiguration() throws IOException {
        return ResourceConfigurationLoader.loadConfigurationFromS3( s3,
                awsLaunchConfig.getBucket(),
                awsLaunchConfig.getFolder(),
                MapboxConfiguration.class );
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
        if ( auth0Configuration.getManagementApiUrl().contains( Auth0Configuration.NO_SYNC_URL ) ) {
            return new LocalUserDirectoryService( auth0Configuration );
        }
        return new Auth0UserDirectoryService( auth0TokenProvider(), hazelcastInstance );
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
        return new Assembler(
                dbcs(),
                hikariDataSource,
                authorizationManager(),
                authorizingComponent(),
                principalService(),
                partitionManager(),
                metricRegistry,
                hazelcastInstance,
                eventBus
        );
    }

    @Bean
    @Profile( Profiles.LOCAL_CONFIGURATION_PROFILE )
    public OrganizationsInitializationDependencies organizationLocalBootstrapDependencies() throws IOException {
        return new OrganizationsInitializationDependencies( organizationsManager(),
                principalService(),
                getLocalConductorConfiguration() );
    }

    @Bean
    @Profile( { Profiles.AWS_CONFIGURATION_PROFILE, Profiles.AWS_TESTING_PROFILE } )
    public OrganizationsInitializationDependencies organizationAwsBootstrapDependencies() throws IOException {
        return new OrganizationsInitializationDependencies( organizationsManager(),
                principalService(),
                getAwsConductorConfiguration() );
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
        return new AssemblerDependencies( hikariDataSource, dbcs(), assemblerConnectionManager() );
    }

    @Bean
    public MaterializedEntitySetsDependencies materializedEntitySetsDependencies() {
        return new MaterializedEntitySetsDependencies(
                assembler(),
                HazelcastMap.MATERIALIZED_ENTITY_SETS.getMap( hazelcastInstance ),
                organizationsManager(),
                dataModelService(),
                authorizingComponent(),
                hikariDataSource
        );
    }

    @Bean
    public MaterializedEntitySetsDataRefreshTask materializedEntitySetsDataRefreshTask() {
        return new MaterializedEntitySetsDataRefreshTask();
    }

    @Bean
    public MaterializePermissionSyncTask materializePermissionSyncTask() {
        return new MaterializePermissionSyncTask();
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

    //    @Bean
    //    public CleanOutOldUsersInitializationTask cleanOutOldUsersInitializationTask() {
    //        return new CleanOutOldUsersInitializationTask();
    //    }

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
                entitySetManager(),
                authorizationManager(),
                partitionManager() );
    }

    @Bean
    public AssemblerConnectionManager assemblerConnectionManager() {
        return new AssemblerConnectionManager( assemblerConfiguration,
                hikariDataSource,
                principalService(),
                organizationsManager(),
                dbcs(),
                eventBus,
                metricRegistry );
    }

    @Bean
    public PhoneNumberService phoneNumberService() {
        return new PhoneNumberService( hazelcastInstance );
    }

    @Bean
    public AssemblerQueryService assemblerQueryService() {
        return new AssemblerQueryService( dataModelService() );
    }

    @Bean
    public HazelcastOrganizationService organizationsManager() {
        return new HazelcastOrganizationService(
                hazelcastInstance,
                aclKeyReservationService(),
                authorizationManager(),
                principalService(),
                phoneNumberService(),
                partitionManager(),
                assembler() );
    }

    @Bean
    public OrganizationsInitializationTask organizationBootstrap() {
        return new OrganizationsInitializationTask();
    }

    @Bean
    public Auth0TokenProvider auth0TokenProvider() {
        return new AwsAuth0TokenProvider( auth0Configuration );
    }

    @Bean
    public Auth0SyncService auth0SyncService() {
        return new Auth0SyncService( hazelcastInstance, hikariDataSource, principalService(), organizationsManager() );
    }

    @Bean
    public UserListingService userListingService() {
        if ( auth0Configuration.getManagementApiUrl().contains( Auth0Configuration.NO_SYNC_URL ) ) {
            return new LocalUserListingService( auth0Configuration );
        }
        return new Auth0UserListingService(
                new ManagementAPI( auth0Configuration.getDomain(),
                auth0TokenProvider().getToken() ) );

    }

    @Bean
    public Auth0SyncTaskDependencies auth0SyncTaskDependencies() {
        return new Auth0SyncTaskDependencies( auth0SyncService(), userListingService(), executor );
    }

    @Bean
    public Auth0SyncTask auth0SyncTask() {
        return new Auth0SyncTask();
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
    public SearchService searchService() {
        return new SearchService( eventBus, metricRegistry );
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
    public PostgresEdmManager pgEdmManager() {
        return new PostgresEdmManager( hikariDataSource, hazelcastInstance );
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
        return () -> HazelcastQueue.EMAIL_SPOOL.getQueue( hazelcastInstance );
    }

    @Bean
    public PostgresEntityDataQueryService dataQueryService() {
        return new PostgresEntityDataQueryService(
                hikariDataSource,
                byteBlobDataManager,
                partitionManager()
        );
    }

    @Bean
    PartitionManager partitionManager() {
        return new PartitionManager( hazelcastInstance, hikariDataSource );
    }

    @Bean
    public EdmManager dataModelService() {
        return new EdmService(
                hikariDataSource,
                hazelcastInstance,
                aclKeyReservationService(),
                authorizationManager(),
                pgEdmManager(),
                entityTypeManager(),
                schemaManager()
        );
    }

    @Bean
    public EntitySetManager entitySetManager() {
        return new EntitySetService(
                hazelcastInstance,
                eventBus,
                pgEdmManager(),
                aclKeyReservationService(),
                authorizationManager(),
                partitionManager(),
                dataModelService(),
                auditingConfiguration
        );
    }

    @Bean
    public GraphService graphService() {
        return new Graph( hikariDataSource, entitySetManager(), partitionManager() );
    }

    @Bean
    public EntityDatastore entityDatastore() {
        return new PostgresEntityDatastore( dataQueryService(), pgEdmManager(), entitySetManager(), metricRegistry );
    }

    @Bean
    public HazelcastIdGenerationService idGenerationService() {
        return new HazelcastIdGenerationService( hazelcastClientProvider, executor );
    }

    @Bean
    public EntityKeyIdService idService() {
        return new PostgresEntityKeyIdService( hazelcastClientProvider,
                executor,
                hikariDataSource,
                idGenerationService(),
                partitionManager() );
    }

    @Bean
    public IndexingMetadataManager indexingMetadataManager() {
        return new IndexingMetadataManager( hikariDataSource, partitionManager() );
    }

    @Bean
    public EdmAuthorizationHelper authorizingComponent() {
        return new EdmAuthorizationHelper( dataModelService(), authorizationManager(), entitySetManager() );
    }

    @Bean
    public AuditInitializationTask auditInitializationTask() {
        return new AuditInitializationTask( hazelcastInstance );
    }

    @Bean
    public PostgresEntitySetSizesTaskDependency postgresEntitySetSizesTaskDependency() {
        return new PostgresEntitySetSizesTaskDependency( hikariDataSource );
    }

    @Bean
    public LinkingQueryService lqs() {
        return new PostgresLinkingQueryService( hikariDataSource, partitionManager() );
    }

    @Bean
    public PostgresLinkingFeedbackService postgresLinkingFeedbackQueryService() {
        return new PostgresLinkingFeedbackService( hikariDataSource, hazelcastInstance );
    }

    @Bean
    public IdGenerationCatchupDependency idgenCatchupDependency() {
        return new IdGenerationCatchupDependency(
                HazelcastMap.ID_GENERATION.getMap( hazelcastClientProvider.getClient( HazelcastClient.IDS.name())),
                hikariDataSource );
    }

    @Bean
    public IdGenerationCatchUpTask idgenCatchupTask() {
        return new IdGenerationCatchUpTask();
    }

    @Bean
    public PostgresMetaDataPropertiesInitializationDependency postgresMetaDataPropertiesInitializationDependency() {
        return new PostgresMetaDataPropertiesInitializationDependency( dataModelService() );
    }

    @Bean
    public PostgresMetaDataPropertiesInitializationTask postgresMetaDataPropertiesInitializationTask() {
        return new PostgresMetaDataPropertiesInitializationTask();
    }

    @Bean
    public GraphQueryService gqs() {
        return new PostgresGraphQueryService( hikariDataSource, entitySetManager(), dataQueryService() );
    }

    @Bean
    public SubscriptionService subscriptionService() {
        return new PostgresSubscriptionService( hikariDataSource, defaultObjectMapper() );
    }

    @Bean
    public SubscriptionNotificationDependencies subscriptionNotificationDependencies() {
        return new SubscriptionNotificationDependencies( hikariDataSource,
                principalService(),
                authorizationManager(),
                authorizingComponent(),
                mailServiceClient(),
                subscriptionService(),
                gqs(),
                HazelcastQueue.TWILIO.getQueue( hazelcastInstance )
        );
    }

    @Bean
    public SubscriptionNotificationTask subscriptionNotificationTask() {
        return new SubscriptionNotificationTask();
    }

    @PostConstruct
    void initPrincipals() {
        Principals.init( principalService(), hazelcastInstance );
    }
}
