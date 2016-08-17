package com.kryptnostic.conductor;

import com.kryptnostic.conductor.pods.ConductorSecurityPod;
import com.kryptnostic.conductor.pods.ConductorServicesPod;
import com.kryptnostic.conductor.pods.ConductorServletsPod;
import com.kryptnostic.mapstores.pods.BaseSerializersPod;
import com.kryptnostic.rhizome.configuration.websockets.BaseRhizomeServer;
import com.kryptnostic.rhizome.pods.hazelcast.RegistryBasedHazelcastInstanceConfigurationPod;

public class ConductorOld extends BaseRhizomeServer {
    public ConductorOld( Class<?>... defaultPods ) {
        super(
                RegistryBasedHazelcastInstanceConfigurationPod.class,
                BaseSerializersPod.class,
                ConductorServletsPod.class,
                ConductorServicesPod.class,
                ConductorSecurityPod.class );
    }

    public static void main( String[] args ) throws Exception {
        new ConductorOld().start( args );
    }

}
