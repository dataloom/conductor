package com.kryptnostic.conductor.pods;

import javax.inject.Inject;

import org.springframework.context.annotation.Configuration;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Session;
import com.kryptnostic.rhizome.cassandra.CassandraTableBuilder;
import com.kryptnostic.rhizome.mapstores.SelfRegisteringMapStore;
import com.kryptnostic.rhizome.mapstores.cassandra.AbstractStructuredCassandraMapstore;

@Configuration
public class ConductorMapstoresPod {
    @Inject
    private Session session;

    public SelfRegisteringMapStore<PermissionQuery, > permissionMapstore() {
        return new AbstractStructuredCassandraMapstore<K, V>(
                "authz",
                session,
                ( K key, BoundStatement bs ) -> bs.bind( key ),
                ( K key, V value, BoundStatement bs ) -> bs.bind( key ),
                new CassandraTableBuilder().partitionKey( new CassandraTableBuilder.ValueColumn( "" , DataType.text() )  ) ) {

                    @Override
                    public K generateTestKey() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public V generateTestValue() throws Exception {
                        // TODO Auto-generated method stub
                        return null;
                    }};
    }
}
