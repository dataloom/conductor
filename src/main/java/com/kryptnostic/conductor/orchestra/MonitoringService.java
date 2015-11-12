package com.kryptnostic.conductor.orchestra;

import java.util.Map;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class MonitoringService {
    private final IMap<String,Set<ServiceDescriptor>> services;
    public MonitoringService( HazelcastInstance hazelcast ) {
        this.services = hazelcast.getMap( "conductorManagedServices" );
    }
    
    @Scheduled(fixedRate=30000)
    public void scan() { 
        Map<String, ServiceStatus> status = services.executeOnEntries( ... );
        
    }
}
