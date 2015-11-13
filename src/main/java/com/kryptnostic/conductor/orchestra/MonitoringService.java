package com.kryptnostic.conductor.orchestra;

import java.util.Map;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.collect.Maps;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.kryptnostic.rhizome.emails.EmailService;

import jodd.mail.Email;
import jodd.mail.MailAddress;

public class MonitoringService {
	private final IMap<String, Set<ServiceDescriptor>> services;
	private final EmailService emailService;

	public MonitoringService(HazelcastInstance hazelcast, EmailService emailService) {
		this.services = hazelcast.getMap("conductorManagedServices");
		this.emailService = emailService;
	}

	@Scheduled(fixedRate = 30000)
	public void scan() {
		@SuppressWarnings("unchecked")
		Map<String, Set<ServiceStatus>> status = Maps.transformEntries(services.executeOnEntries(new HealthScanEntryProcessor()),
				(String key, Object value) -> {
					return (Set<ServiceStatus>) value;
				});
		report(status);
	}

	private void report(Map<String, Set<ServiceStatus>> status) {

		Email email = new Email();
		MailAddress address = new MailAddress("yao@kryptnostic.com");
		email.setTo(address);
		

		status.forEach((String serviceName, Set<ServiceStatus> serviceStatus) -> {
			for(ServiceStatus ss : serviceStatus){
				if (!ss.isConnectable()) {
					email.addText(ss.toString());
					emailService.sendMessage(email);
				}
			}
			
		});

	}
}
