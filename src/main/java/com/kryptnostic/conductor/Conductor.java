package com.kryptnostic.conductor;

import java.util.concurrent.ExecutionException;

import com.kryptnostic.conductor.pods.ConductorSparkPod;
import com.kryptnostic.conductor.pods.ConductorStreamSerializersPod;
import com.kryptnostic.mapstores.pods.BaseSerializersPod;
import com.kryptnostic.rhizome.core.RhizomeApplicationServer;
import com.kryptnostic.rhizome.hazelcast.serializers.RhizomeUtils.Pods;
import com.kryptnostic.rhizome.pods.CassandraPod;
import com.kryptnostic.rhizome.pods.hazelcast.RegistryBasedHazelcastInstanceConfigurationPod;

/**
 * This class will not run unless ./gradlew :kindling:clean :kindling:build :kindling:shadow --daemon has been run in
 * super project. You must also download Spark 1.6.2 w/ Hadoop and have a master and slave running locally. Finally you
 * must make sure to update {@link ConductorSparkPod} with your spark master URL.
 *
 * @author Matthew Tamayo-Rios &lt;matthew@kryptnostic.com&gt;
 */
public class Conductor extends RhizomeApplicationServer {
    public static final Class<?>[] rhizomePods   = new Class<?>[] {
            CassandraPod.class,
            BaseSerializersPod.class,
            RegistryBasedHazelcastInstanceConfigurationPod.class };

    public static final Class<?>[] conductorPods = new Class<?>[] {
            ConductorSparkPod.class,
            ConductorStreamSerializersPod.class,
            CassandraPod.class
    };

    public Conductor() {
        super( Pods.concatenate( RhizomeApplicationServer.defaultPods, rhizomePods, conductorPods ) );
    }

    public static void main( String[] args ) throws InterruptedException, ExecutionException {
        new Conductor().sprout( args );
        System.out.println( "WE DID IT!\n\n\n\n\n MISSION ACCOMPLISHED!!!" );
    }
}
