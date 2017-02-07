package com.kryptnostic.conductor.pods;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dataloom.authorization.AceKey;
import com.dataloom.authorization.DelegatedPermissionEnumSet;
import com.dataloom.authorization.SecurableObjectType;
import com.dataloom.authorization.mapstores.SecurableObjectTypeMapstore;
import com.dataloom.authorization.mapstores.PermissionMapstore;
import com.datastax.driver.core.Session;
import com.kryptnostic.rhizome.mapstores.SelfRegisteringMapStore;

@Configuration
public class ConductorMapstoresPod {
    @Inject
    private Session session;

    @Bean
    public SelfRegisteringMapStore<AceKey, DelegatedPermissionEnumSet> permissionMapstore() {
        return new PermissionMapstore( session );
    }
    
    @Bean
    public SelfRegisteringMapStore<List<UUID>, SecurableObjectType> securableObjectTypeMapstore() {
        return new SecurableObjectTypeMapstore( session );
    }
}
