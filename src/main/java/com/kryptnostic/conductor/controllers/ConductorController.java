package com.kryptnostic.conductor.controllers;

import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.codahale.metrics.annotation.Timed;

@Controller
public class ConductorController {

    @RequestMapping(
        value = { "", "/" },
        method = RequestMethod.GET,
        produces = MediaType.TEXT_PLAIN )
    @Timed
    public @ResponseBody String helloWorld() {
        return "Hello World!";
    }

}
