package com.kryptnostic.conductor.orchestra;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@Component
public class MonitoringService {
	private final IMap<String, Set<ServiceDescriptor>> services;

	@Inject
	public MonitoringService(HazelcastInstance hazelcast) {
		this.services = hazelcast.getMap("conductorManagedServices");
	}

	@Scheduled(fixedRate = 30000)
	public void check() {
		services.executeOnEntries(new MonitoringServiceEntryProcessor());
	}
}
