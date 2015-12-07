package com.kryptnostic.conductor.controllers;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.kryptnostic.conductor.orchestra.ConductorApi;
import com.kryptnostic.conductor.orchestra.ServiceRegistrationService;
import com.kryptnostic.conductor.v1.objects.ServiceDescriptor;

@Controller
@RequestMapping( ConductorApi.MONITORING )
public class ConductorController extends DefaultController implements ConductorApi {

    @Inject
    private ServiceRegistrationService srs;

    @Override
    @RequestMapping(
        value = REGISTRATION,
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON )
    @ResponseStatus( HttpStatus.OK )
    public void setRegistration( @RequestBody ServiceDescriptor desc ) {
        srs.register( desc );
    }

    @Override
    @RequestMapping(
        value = HEALTH,
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON )
    @ResponseStatus( HttpStatus.OK )
    @ResponseBody
    public void checkHealth() {}

}
