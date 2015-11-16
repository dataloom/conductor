package com.kryptnostic.conductor.orchestra;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.kryptnostic.rhizome.hazelcast.processors.AbstractRhizomeEntryProcessor;

class MonitoringServiceEntryProcessor extends AbstractRhizomeEntryProcessor<String, Set<ServiceDescriptor>> {
	 

	private static final long 				serialVersionUID = 5810253239672565698L;
	

	@Override
	public Set<ServiceStatus> process(Map.Entry<String, Set<ServiceDescriptor>> entry) {
		
		Set<ServiceDescriptor> desc = entry.getValue();
		Set<ServiceStatus> res = new HashSet<>();
		
		for(ServiceDescriptor item : desc){
			
			String pingBackUrl = item.getPingbackUrl();
			
			try {
				URL url = new URL(pingBackUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				res.add( new ServiceStatus(item, true));
				conn.disconnect();
				
			} catch (IOException e) {
				res.add(new ServiceStatus(item, false));
			}
		}
		return res;
	}
	
}