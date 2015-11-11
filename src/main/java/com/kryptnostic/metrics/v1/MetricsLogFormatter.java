package com.kryptnostic.metrics.v1;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class MetricsLogFormatter extends Formatter {
    private static SimpleFormatter formatter;

    public MetricsLogFormatter() {
        formatter = new SimpleFormatter();
    }

    @Override
    public String format( LogRecord record ) {
        return formatter.format( record );
    }

}
