package com.kryptnostic.metrics.v1.userstats;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public final class MetricsDate {
    protected static String DATE_FORMAT = "MM/dd/yyyy";

    public static LocalDate getDate() {
        new DateTime();
        return DateTime.now().toLocalDate();
    }

    public static String getDay( LocalDate date ) {
        DateTimeFormatter dtf = DateTimeFormat.forPattern( DATE_FORMAT );
        return date.toString( dtf );
    }
}
