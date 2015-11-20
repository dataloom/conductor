package com.kryptnostic.conductor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.geekbeast.rhizome.configuration.Configuration;
import com.geekbeast.rhizome.configuration.ConfigurationKey;
import com.geekbeast.rhizome.configuration.SimpleConfigurationKey;
import com.google.common.base.Optional;

public class ConductorConfiguration implements Configuration {
    private static final long             serialVersionUID           = 1L;
    private static final ConfigurationKey key                        = new SimpleConfigurationKey( "conductor.yaml" );
    private static final String           REPORT_EMAIL_ADDRESS_FIELD = "reportEmailAddress";

    private final String                  reportEmailAddress;

    @JsonCreator
    public ConductorConfiguration(
            @JsonProperty( REPORT_EMAIL_ADDRESS_FIELD ) Optional<String> reportEmailAddress) {
        this.reportEmailAddress = reportEmailAddress.or( "yao@kryptnostic.com" );
    }

    @JsonProperty( REPORT_EMAIL_ADDRESS_FIELD )
    public String getReportEmailAddress() {
        return reportEmailAddress;
    }

    @Override
    @JsonIgnore
    public ConfigurationKey getKey() {
        return key;
    }

    @JsonIgnore
    public static ConfigurationKey key() {
        return key;
    }

}
