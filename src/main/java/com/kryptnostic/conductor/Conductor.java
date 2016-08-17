package com.kryptnostic.conductor;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.pods.ConductorSparkPod;
import com.kryptnostic.conductor.pods.ConductorStreamSerializers;
import com.kryptnostic.rhizome.core.RhizomeApplicationServer;

public class Conductor extends RhizomeApplicationServer {
    public Conductor() {
        super();
        intercrop( ConductorSparkPod.class, ConductorStreamSerializers.class );
    }

    public static void main( String[] args ) throws InterruptedException, ExecutionException {
        Conductor c = new Conductor();
        c.sprout();
        HazelcastInstance hazelcast = c.getContext().getBean( HazelcastInstance.class );
        Future<List<Employee>> emps = hazelcast.getExecutorService( "default" ).submit( new ConductorCall() {
            private static final long serialVersionUID = 2L;

            @Override
            public List<Employee> call() throws Exception {
                return api.processEmployees();
            }

        } );
        
        System.out.println( emps.get() );

    }
}
