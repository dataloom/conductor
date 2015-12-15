package com.kryptnostic.conductor.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.kryptnostic.conductor.Conductor;
import com.kryptnostic.conductor.orchestra.MonitoringService;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;
import com.kryptnostic.conductor.v1.objects.ServiceDescriptor;
import com.kryptnostic.conductor.v1.objects.ServiceDescriptorSet;
import com.kryptnostic.mapstores.v1.constants.HazelcastNames.Maps;

public class ConductorTests {

    private final static Logger                       logger            = LoggerFactory
                                                                                .getLogger( ConductorTests.class );
    private static final String                       pingbackUrl       = "http://localhost:8085/conductor/monitoring/health";
    private static Conductor                          conductor         = null;
    private static ServiceRegistrationService         srs               = null;
    private static IMap<String, ServiceDescriptorSet> services          = null;
    private static ServiceDescriptor                  serviceDescriptor = null;
    private static HazelcastInstance                  hazelcastInstance = null;
    private static MonitoringService                  monitoringService = null;

    @BeforeClass
    public static void initTests() throws Exception {
        conductor = new Conductor();
        conductor.start();
        logger.info( "Successfully started Conductor Server." );
        hazelcastInstance = conductor.getContext().getBean( HazelcastInstance.class );
        srs = conductor.getContext().getBean( ServiceRegistrationService.class );
        monitoringService = conductor.getContext().getBean( MonitoringService.class );
        services = hazelcastInstance.getMap( Maps.CONDUCTOR_MANAGED_SERVICES );
        serviceDescriptor = new ServiceDescriptor(
                "MonitoringService",
                "localhost",
                8085,
                pingbackUrl,
                "Empty for now" );
    }

    @AfterClass
    public static void tearDownTests() throws BeansException, Exception {
        logger.info( "Finished testing ConductorTests" );
        conductor.stop();
        logger.info( "Successfully shutdown Conductor Server, exiting main thread" );
    }

    @Test
    public void serviceRegistrationServiceTest() {
        srs.register( serviceDescriptor );
        ServiceDescriptorSet sdSet = services.get( serviceDescriptor.getServiceName() );
        assertTrue( sdSet.contains( serviceDescriptor ) );

    }

    // TODO: write normal test for MonitoringService
    @Test
    public void monitoringServiceTest() throws IOException {
        assertTrue( true );
    }

}
