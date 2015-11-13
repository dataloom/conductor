package com.kryptnostic.conductor.orchestra;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.kryptnostic.rhizome.hazelcast.processors.AbstractRhizomeEntryProcessor;

class HealthScanEntryProcessor extends AbstractRhizomeEntryProcessor<String, Set<ServiceDescriptor>> {
	 

	private static final long serialVersionUID = 5810253239672565698L;

	@Override
	public Set<ServiceStatus> process(Map.Entry<String, Set<ServiceDescriptor>> entry) {
		
		Set<ServiceDescriptor> desc = entry.getValue();
		Set<ServiceStatus> res = new HashSet<>();
		for(ServiceDescriptor item : desc){
			String name = item.getName();
			String host = item.getHost();
			int port = item.getPort();
			try {
				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(host, port), 5000);
				socket.close();
				res.add( new ServiceStatus(name, host, port, true));
				
			} catch (IOException e) {
				res.add(new ServiceStatus(name, host, port, false));
			}
		}
		return res;
	}
	
}