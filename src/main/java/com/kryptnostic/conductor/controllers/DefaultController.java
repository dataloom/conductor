package com.kryptnostic.conductor.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

// TODO: write a BasicResponse class
public class DefaultController {

    private static Logger logger = LoggerFactory.getLogger( DefaultController.class );

    @ExceptionHandler( IllegalArgumentException.class )
    @ResponseStatus( HttpStatus.BAD_REQUEST )
    @ResponseBody
    public String handleBadRequest( Exception ex ) {
        return ex.getMessage();
    }

    @ExceptionHandler( Exception.class )
    @ResponseStatus( HttpStatus.INTERNAL_SERVER_ERROR )
    @ResponseBody
    public String handleException( Exception ex ) {
        logger.error( "Controller: {} Error: {}", this.getClass().getName(), ex.getMessage() );
        return ex.getMessage();
    }

}
