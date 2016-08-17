package com.kryptnostic.conductor;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class ConductorCall implements Callable<List<Employee>>, Serializable {
    private static final long serialVersionUID = 1L;
    protected ConductorSparkApi api;
    
    @Override
    public abstract List<Employee> call() throws Exception;

    public void setApi( ConductorSparkApi api ) {
        this.api = api ;
    }
}
