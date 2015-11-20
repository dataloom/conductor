package com.kryptnostic.conductor.orchestra;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.kryptnostic.conductor.ConductorConfiguration;
import com.kryptnostic.conductor.v1.objects.ServiceDescriptorSet;
import com.kryptnostic.conductor.v1.processors.MonitoringServiceEntryProcessor;
import com.kryptnostic.mapstores.v1.constants.HazelcastNames.Maps;

@Component
public class MonitoringService {
    private final IMap<String, ServiceDescriptorSet> services;
    private final String                             hazelcastInstanceName;
    private final String                             reportEmailAddress;

    @Inject
    private ConductorConfiguration                   conductorConfig;

    @Inject
    public MonitoringService( HazelcastInstance hazelcast ) {
        this.services = hazelcast.getMap( Maps.CONDUCTOR_MANAGED_SERVICES );
        this.hazelcastInstanceName = hazelcast.getName();
        this.reportEmailAddress = conductorConfig.getReportEmailAddress();
    }

    @Scheduled(
        fixedRate = 30000 )
    public void check() throws IOException {
        services.executeOnEntries( new MonitoringServiceEntryProcessor( hazelcastInstanceName, reportEmailAddress ) );
    }

}
