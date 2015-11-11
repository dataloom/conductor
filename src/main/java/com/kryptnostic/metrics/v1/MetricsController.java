package com.kryptnostic.metrics.v1;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.codahale.metrics.annotation.Timed;
import com.kryptnostic.instrumentation.v1.MetricsApi;
import com.kryptnostic.instrumentation.v1.exceptions.types.BadRequestException;
import com.kryptnostic.instrumentation.v1.models.BasicResponse;
import com.kryptnostic.instrumentation.v1.models.MetricsObject;

@Controller
@RequestMapping( MetricsApi.CONTROLLER )
public class MetricsController implements MetricsApi {

    @Inject
    private MetricsService metricsService;

    @Override
    @RequestMapping(
        value = { "", "/" },
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON,
        produces = MediaType.APPLICATION_JSON )
    @Timed
    public @ResponseBody BasicResponse<String> log( @RequestBody MetricsObject met ) throws BadRequestException {
        metricsService.log( met );
        return new BasicResponse<String>( "Object logged", HttpStatus.OK_200, true );
    }

}
