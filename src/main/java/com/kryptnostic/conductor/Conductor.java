package com.kryptnostic.conductor;

import java.util.concurrent.ExecutionException;

import com.kryptnostic.conductor.pods.ConductorSparkPod;
import com.kryptnostic.conductor.pods.ConductorStreamSerializersPod;
import com.kryptnostic.rhizome.core.RhizomeApplicationServer;
import com.kryptnostic.rhizome.pods.CassandraPod;

/**
 * This class will not run unless ./gradlew :kindling:clean :kindling:build :kindling:shadow --daemon has been run in
 * super project. You must also download Spark 1.6.2 w/ Hadoop and have a master and slave running locally. Finally you
 * must make sure to update {@link ConductorSparkPod} with your spark master URL.
 *
 * @author Matthew Tamayo-Rios &lt;matthew@kryptnostic.com&gt;
 */
public class Conductor extends RhizomeApplicationServer {
    public Conductor() {
        super();
        intercrop( ConductorSparkPod.class,
                ConductorStreamSerializersPod.class,
                CassandraPod.class );
    }

    public static void main( String[] args ) throws InterruptedException, ExecutionException {
        Conductor c = new Conductor();
        c.sprout();
    }
}
