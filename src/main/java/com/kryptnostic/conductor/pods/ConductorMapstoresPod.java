package com.kryptnostic.conductor.pods;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dataloom.authorization.AceKey;
import com.dataloom.authorization.DelegatedPermissionEnumSet;
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
}
