package com.kryptnostic.metrics.v1;

import com.kryptnostic.instrumentation.v1.models.MetricsObject;

public interface MetricsService {

    void log( MetricsObject met );
    
}
