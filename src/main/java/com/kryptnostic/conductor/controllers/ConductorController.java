package com.kryptnostic.conductor.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.orchestra.ConductorApi;
import com.kryptnostic.conductor.orchestra.ServiceDescriptor;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;

@Controller
@RequestMapping(ConductorApi.CONTROLLER)
public class ConductorController {
    
    @RequestMapping(
    	value = "/registration",
    	method = RequestMethod.POST)
    @ResponseBody
    public void setRegistration(@RequestBody HazelcastInstance hazelcast, ServiceDescriptor desc) {
    	
    	ServiceRegistrationService srs = new ServiceRegistrationService(hazelcast);
    	srs.register(desc);
    	
    }
    

}
