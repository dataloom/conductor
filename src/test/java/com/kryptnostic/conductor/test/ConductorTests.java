package com.kryptnostic.conductor.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import com.geekbeast.rhizome.tests.configurations.JacksonConverter;
import com.kryptnostic.conductor.Conductor;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;
import com.kryptnostic.conductor.v1.objects.ServiceDescriptor;

import retrofit.RestAdapter;

public class ConductorTests {
    private final static Logger               logger                     = LoggerFactory
                                                                                 .getLogger( ConductorTests.class );
    private static RestAdapter                adapter;
    private static final String               localhostUrl               = "http://localhost:8084/v1";

    private static Conductor                  conductor                  = null;
    private static ServiceRegistrationService serviceRegistrationService = null;

    @BeforeClass
    public static void initTests() throws Exception {
        conductor = new Conductor();
        conductor.start();
        serviceRegistrationService = conductor.getContext().getBean( ServiceRegistrationService.class );
        logger.info( "Successfully started Conductor microservice." );
        adapter = new RestAdapter.Builder().setEndpoint( "" )
                .setConverter( new JacksonConverter() ).build();
    }

    @AfterClass
    public static void tearDownTests() throws BeansException, Exception {
        logger.info( "Finished testing ConductorTests" );
        conductor.stop();
        logger.info( "Successfully shutdown Jetty, exiting main thread" );
    }

    @Test
    public void readWriteConfigurationTest() {

    }

    @Test
    public void serviceRegistrationServiceTest() {
        ServiceDescriptor serviceDescriptor = new ServiceDescriptor(
                "MonitoringService",
                "localhost",
                8084,
                localhostUrl + "/conductor" + "/health",
                "Empty for now" );
        serviceRegistrationService.register( serviceDescriptor );
    }

    @Test
    public void monitoringServiceTest() {

    }

}
