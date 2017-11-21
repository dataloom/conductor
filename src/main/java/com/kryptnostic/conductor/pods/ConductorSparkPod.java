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

import com.dataloom.authorization.AbstractSecurableObjectResolveTypeService;
import com.dataloom.authorization.AuthorizationManager;
import com.dataloom.authorization.AuthorizationQueryService;
import com.dataloom.authorization.HazelcastAbstractSecurableObjectResolveTypeService;
import com.dataloom.authorization.HazelcastAclKeyReservationService;
import com.dataloom.authorization.HazelcastAuthorizationService;
import com.dataloom.data.DatasourceManager;
import com.dataloom.edm.properties.PostgresTypeManager;
import com.dataloom.edm.schemas.SchemaQueryService;
import com.dataloom.edm.schemas.manager.HazelcastSchemaManager;
import com.dataloom.edm.schemas.postgres.PostgresSchemaQueryService;
import com.dataloom.hazelcast.HazelcastQueue;
import com.dataloom.linking.HazelcastBlockingService;
import com.dataloom.linking.HazelcastMergingService;
import com.dataloom.mail.config.MailServiceRequirements;
import com.dataloom.mappers.ObjectMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.rpc.ConductorConfiguration;
import com.kryptnostic.conductor.rpc.ConductorElasticsearchApi;
import com.kryptnostic.datastore.services.EdmManager;
import com.kryptnostic.datastore.services.EdmService;
import com.kryptnostic.datastore.services.PostgresEntitySetManager;
import com.kryptnostic.kindling.search.ConductorElasticsearchImpl;
import com.kryptnostic.rhizome.core.Cutting;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.UnknownHostException;
import javax.inject.Inject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConductorSparkPod {

    @Inject
    private HazelcastInstance hazelcastInstance;

    @Inject
    private ConductorConfiguration conductorConfiguration;

    @Inject
    private EventBus eventBus;

    @Inject
    private Cutting cutting;

    @Inject
    private HikariDataSource hikariDataSource;

    @Inject
    private ListeningExecutorService executorService;

    @Bean
    public ObjectMapper defaultObjectMapper() {
        return ObjectMappers.getJsonMapper();
    }

    @Bean
    public AuthorizationQueryService authorizationQueryService() {
        return new AuthorizationQueryService( hikariDataSource, hazelcastInstance );
    }

    @Bean
    public AuthorizationManager authorizationManager() {
        return new HazelcastAuthorizationService( hazelcastInstance, authorizationQueryService(), eventBus );
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
    public PostgresEntitySetManager entitySetManager() {
        return new PostgresEntitySetManager( hikariDataSource );
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
    public HazelcastAclKeyReservationService aclKeyReservationService() {
        return new HazelcastAclKeyReservationService( hazelcastInstance );
    }

    @Bean
    public MailServiceRequirements mailServiceRequirements() {
        return () -> hazelcastInstance.getQueue( HazelcastQueue.EMAIL_SPOOL.name() );
    }

    @Bean
    public DatasourceManager datasourceManager() {
        return new DatasourceManager( hikariDataSource, hazelcastInstance );
    }

    @Bean
    public EdmManager dataModelService() {
        return new EdmService(
                hikariDataSource,
                hazelcastInstance,
                aclKeyReservationService(),
                authorizationManager(),
                entitySetManager(),
                entityTypeManager(),
                schemaManager(),
                datasourceManager() );
    }

    @Bean
    public ConductorElasticsearchApi elasticsearchApi() throws UnknownHostException, IOException {
        return new ConductorElasticsearchImpl( conductorConfiguration.getSearchConfiguration() );
    }

    @Bean
    public HazelcastBlockingService blockingService() {
        return new HazelcastBlockingService( hazelcastInstance );
    }

    @Bean
    public HazelcastMergingService mergingService() {
        return new HazelcastMergingService( hazelcastInstance, executorService );
    }
}
