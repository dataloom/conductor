package com.kryptnostic.conductor.pods;

import java.util.EnumSet;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dataloom.authorization.AceKey;
import com.dataloom.authorization.mapstores.PermissionMapstore;
import com.dataloom.authorization.requests.Permission;
import com.datastax.driver.core.Session;
import com.kryptnostic.rhizome.mapstores.SelfRegisteringMapStore;

@Configuration
public class ConductorMapstoresPod {
    @Inject
    private Session session;

    @Bean
    public SelfRegisteringMapStore<AceKey, EnumSet<Permission>> permissionMapstore() {
        return new PermissionMapstore( session );
    }
}
