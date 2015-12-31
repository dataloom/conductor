package com.kryptnostic.conductor;

import com.kryptnostic.conductor.pods.ConductorSecurityPod;
import com.kryptnostic.conductor.pods.ConductorServicesPod;
import com.kryptnostic.conductor.pods.ConductorServletsPod;
import com.kryptnostic.mapstores.pods.SerializersPod;
import com.kryptnostic.rhizome.configuration.websockets.BaseRhizomeServer;
import com.kryptnostic.rhizome.pods.hazelcast.RegistryBasedHazelcastInstanceConfigurationPod;

public class Conductor extends BaseRhizomeServer {
    public Conductor( Class<?>... defaultPods ) {
        super(
                RegistryBasedHazelcastInstanceConfigurationPod.class,
                SerializersPod.class,
                ConductorServletsPod.class,
                ConductorServicesPod.class,
                ConductorSecurityPod.class );
    }

    public static void main( String[] args ) throws Exception {
        new Conductor().start( args );
    }

}
