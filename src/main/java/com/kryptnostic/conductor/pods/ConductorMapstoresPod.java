package com.kryptnostic.conductor.pods;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Configuration;

import com.dataloom.authorization.AceKey;
import com.dataloom.authorization.mapstores.PermissionMapstore;
import com.dataloom.authorization.requests.Permission;
import com.dataloom.edm.internal.DatastoreConstants;
import com.datastax.driver.core.Session;
import com.kryptnostic.datastore.cassandra.CommonColumns;
import com.kryptnostic.rhizome.cassandra.CassandraTableBuilder;
import com.kryptnostic.rhizome.mapstores.SelfRegisteringMapStore;

@Configuration
public class ConductorMapstoresPod {
    @Inject
    private Session session;

    public SelfRegisteringMapStore<AceKey, Set<Permission>> permissionMapstore() {
        return new PermissionMapstore(
                session,
                new CassandraTableBuilder( DatastoreConstants.KEYSPACE, PermissionMapstore.MAP_NAME )
                        .partitionKey( CommonColumns.SECURABLE_OBJECT_TYPE, CommonColumns.SECURABLE_OBJECTID )
                        .clusteringColumns( CommonColumns.PRINCIPAL_TYPE, CommonColumns.PRINCIPAL_ID )
                        .columns( CommonColumns.PERMISSIONS ) );
    }
}
