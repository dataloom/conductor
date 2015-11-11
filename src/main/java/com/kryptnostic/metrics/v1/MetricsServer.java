package com.kryptnostic.metrics.v1;

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
import com.kryptnostic.metrics.v1.pods.MetricsSecurityPod;
import com.kryptnostic.metrics.v1.pods.MetricsServicesPod;
import com.kryptnostic.metrics.v1.pods.MetricsServletsPod;
import com.kryptnostic.services.v1.pods.RethinkDbMapStoresPod;

public class MetricsServer {

    private final Rhizome rhizome;

    public MetricsServer() {
        this( new Class<?>[] { ConfigurationPod.class, MetricsPod.class, AsyncPod.class, HazelcastPod.class,
                ServletContainerPod.class, RegistryBasedHazelcastInstanceConfigurationPod.class } );
    }

    public MetricsServer( Class<?>[] defaultPods ) {
        this( defaultPods, MetricsServletsPod.class,
                MetricsServicesPod.class,
                MetricsSecurityPod.class,
                RethinkDbMapStoresPod.class,
                RethinkDbPod.class);
    }

    public MetricsServer( Class<?>[] defaultPods, Class<?>... pods ) {
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
        new MetricsServer().start( args );
    }

    public AnnotationConfigWebApplicationContext getContext() {
        return rhizome.getContext();
    }

}
