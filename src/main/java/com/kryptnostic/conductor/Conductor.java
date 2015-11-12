package com.kryptnostic.conductor;

import org.springframework.beans.BeansException;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.geekbeast.rhizome.core.Rhizome;
import com.geekbeast.rhizome.pods.AsyncPod;
import com.geekbeast.rhizome.pods.ConfigurationPod;
import com.geekbeast.rhizome.pods.HazelcastPod;
import com.geekbeast.rhizome.pods.MetricsPod;
import com.geekbeast.rhizome.pods.RethinkDbPod;
import com.geekbeast.rhizome.pods.ServletContainerPod;
import com.geekbeast.rhizome.pods.hazelcast.RegistryBasedHazelcastInstanceConfigurationPod;
import com.kryptnostic.conductor.pods.ConductorServicesPod;
import com.kryptnostic.conductor.pods.ConductorServletsPod;
import com.kryptnostic.mapstores.pods.SerializersPod;
import com.kryptnostic.services.v1.pods.RethinkDbMapStoresPod;

public class Conductor {

    private final Rhizome rhizome;

    public Conductor() {
        this( new Class<?>[] { ConfigurationPod.class, MetricsPod.class, AsyncPod.class, HazelcastPod.class,
                ServletContainerPod.class, RegistryBasedHazelcastInstanceConfigurationPod.class } );
    }

    public Conductor( Class<?>[] defaultPods ) {
        this( defaultPods,
                SerializersPod.class,
                ConductorServletsPod.class,
                ConductorServicesPod.class,
                ConductorSecurityPod.class,
                RethinkDbMapStoresPod.class,
                RethinkDbPod.class );
    }

    public Conductor( Class<?>[] defaultPods, Class<?>... pods ) {
        rhizome = new Rhizome( pods ) {
            @Override
            public Class<?>[] getDefaultPods() {
                return defaultPods;
            }
        };

    }

    public void start( String... activeProfiles ) throws Exception {
        rhizome.sprout( activeProfiles );
    }

    public void stop() throws BeansException, Exception {
        rhizome.wilt();
    }

    public static void main( String[] args ) throws Exception {
        new Conductor().start( args );
    }

    public AnnotationConfigWebApplicationContext getContext() {
        return rhizome.getContext();
    }

}
