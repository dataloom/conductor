package com.kryptnostic.conductor.orchestra;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.kryptnostic.rhizome.emails.EmailService;
import com.kryptnostic.rhizome.hazelcast.processors.AbstractRhizomeEntryProcessor;

import jodd.mail.Email;
import jodd.mail.MailAddress;

class MonitoringServiceEntryProcessor extends AbstractRhizomeEntryProcessor<String, Set<ServiceDescriptor>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2423765356049257683L;
	
	@Inject
	private EmailService emailService;

	@Override
	public Object process(Map.Entry<String, Set<ServiceDescriptor>> entry) {
		
		Set<ServiceDescriptor> desc = entry.getValue();
		
		for(ServiceDescriptor item : desc){
			
			String pingBackUrl = item.getPingbackUrl();
			
			try {
				URL url = new URL(pingBackUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.disconnect();
				
			} catch (IOException e) {
				Email email = new Email();
				MailAddress address = new MailAddress("yao@kryptnostic.com");
				email.setTo(address);
				email.addText("Service down: " + item.toString());
				emailService.sendMessage(email);
			}
		}
		return null;
	}
	
}