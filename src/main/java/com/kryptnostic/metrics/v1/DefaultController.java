package com.kryptnostic.metrics.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.kryptnostic.instrumentation.v1.exceptions.types.ResourceNotFoundException;
import com.kryptnostic.instrumentation.v1.models.BasicResponse;

public class DefaultController {
    Logger                     logger                = LoggerFactory.getLogger( DefaultController.class );

    public static final String APPLICATION_JSON_UTF8 = "application/json";

    @ExceptionHandler( ResourceNotFoundException.class )
    @ResponseStatus( HttpStatus.NOT_FOUND )
    public @ResponseBody BasicResponse<String> handleResourceNotFound( Exception ex ) {
        return new BasicResponse<String>( ex.getMessage(), HttpStatus.NOT_FOUND.value(), false );
    }

    @ExceptionHandler( IllegalArgumentException.class )
    @ResponseStatus( HttpStatus.BAD_REQUEST )
    public @ResponseBody BasicResponse<String> handleBadRequest( Exception ex ) {
        return new BasicResponse<String>( ex.getMessage(), HttpStatus.BAD_REQUEST.value(), false );
    }

    @ExceptionHandler( Exception.class )
    @ResponseStatus( HttpStatus.INTERNAL_SERVER_ERROR )
    public @ResponseBody BasicResponse<String> handleException( Exception ex ) {
        logger.error( "Controller: {} Error: {}", this.getClass().getName(), ex.getMessage() );
        return new BasicResponse<String>( ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), false );
    }
}
