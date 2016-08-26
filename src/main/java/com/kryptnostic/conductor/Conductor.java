package com.kryptnostic.conductor;

import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.pods.ConductorSparkPod;
import com.kryptnostic.conductor.pods.ConductorStreamSerializersPod;
import com.kryptnostic.conductor.rpc.Lambdas;
import com.kryptnostic.rhizome.core.RhizomeApplicationServer;
import org.apache.spark.api.java.function.VoidFunction;
import com.kryptnostic.conductor.rpc.Employee;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This class will not run unless ./gradlew :kindling:clean :kindling:build :kindling:shadow --daemon  has been run in super project.
 * You must also download Spark 1.6.2 w/ Hadoop and have a master and slave running locally.
 * Finally you must make sure to update {@link ConductorSparkPod} with your spark master URL.
 * @author Matthew Tamayo-Rios &lt;matthew@kryptnostic.com&gt; 
 *
 */
public class Conductor extends RhizomeApplicationServer {
    public Conductor() {
        super();
        intercrop( ConductorSparkPod.class, ConductorStreamSerializersPod.class );
    }

    public static void main( String[] args ) throws InterruptedException, ExecutionException {
        Conductor c = new Conductor();
        c.sprout();
        HazelcastInstance hazelcast = c.getContext().getBean( HazelcastInstance.class );
//        Future<List<Employee>> emps = hazelcast.getExecutorService( "default" ).submit( Lambdas.getEmployees() );

//        System.out.println( "Received back " + emps.get().size() + " employees");
    }

    public static VoidFunction<String> bullshit() {
        return (VoidFunction<String> & Serializable) l -> System.out.println( l );
    }
}
