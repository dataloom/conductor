package com.kryptnostic.conductor.controllers;


import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.kryptnostic.conductor.orchestra.ConductorApi;
import com.kryptnostic.conductor.orchestra.ServiceDescriptor;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;

@Controller
@RequestMapping(ConductorApi.CONTROLLER)
public class ConductorController extends DefaultController implements ConductorApi {
	
	@Inject
	private ServiceRegistrationService srs;
    
    @RequestMapping(
    	value = CONTROLLER + REGISTRATION,
    	method = RequestMethod.POST)
    @ResponseBody
    public void setRegistration(@RequestBody ServiceDescriptor desc) {
    	
    	srs.register(desc);
    	
    }
    

}
