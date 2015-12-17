package com.kryptnostic.conductor;

import com.geekbeast.rhizome.configuration.websockets.RhizomeServerBase;
import com.geekbeast.rhizome.pods.hazelcast.RegistryBasedHazelcastInstanceConfigurationPod;
import com.kryptnostic.conductor.pods.ConductorSecurityPod;
import com.kryptnostic.conductor.pods.ConductorServicesPod;
import com.kryptnostic.conductor.pods.ConductorServletsPod;
import com.kryptnostic.mapstores.pods.SerializersPod;

public class Conductor extends RhizomeServerBase {
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
