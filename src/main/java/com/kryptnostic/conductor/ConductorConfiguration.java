package com.kryptnostic.conductor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.geekbeast.rhizome.configuration.Configuration;
import com.geekbeast.rhizome.configuration.ConfigurationKey;
import com.geekbeast.rhizome.configuration.SimpleConfigurationKey;
import com.google.common.base.Optional;
import com.kryptnostic.instrumentation.v1.constants.InstrumentationConstants;

public class ConductorConfiguration implements Configuration {

    private static final long             serialVersionUID = -3820377783487017980L;

    private static final ConfigurationKey key              = new SimpleConfigurationKey( "metrics.yaml" );

    private final Optional<String>        elasticsearchUrl;
    private final Optional<String>        elasticsearchCluster;

    public ConductorConfiguration(
            @JsonProperty( InstrumentationConstants.ELASTICSEARCH_URL ) Optional<String> elasticsearchUrl,
            @JsonProperty( InstrumentationConstants.ELASTICSEARCH_CLUSTER ) Optional<String> elasticsearchCluster ) {
        this.elasticsearchUrl = elasticsearchUrl;
        this.elasticsearchCluster = elasticsearchCluster;
    }

    @JsonProperty( InstrumentationConstants.ELASTICSEARCH_URL )
    public Optional<String> getElasticsearchUrl() {
        return elasticsearchUrl;
    }

    @JsonProperty( InstrumentationConstants.ELASTICSEARCH_CLUSTER )
    public Optional<String> getElasticsearchCluster() {
        return elasticsearchCluster;
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
