package com.kryptnostic.metrics.v1;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.google.common.collect.ImmutableList;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.metrics.v1.serialization.MetricsLogRecordStreamSerializer;
import com.kryptnostic.metrics.v1.serialization.MetricsUserStatsMetadataStreamSerializer;

public class HazelcastTestHarness {

    protected static HazelcastInstance hazelcast = null;

    @BeforeClass
    public static final void initHazelcast() {
        if ( hazelcast == null ) {
            Config config = new Config( "test" );
            config.setGroupConfig( new GroupConfig( "test", "osterone" ) );
            config.setNetworkConfig( new NetworkConfig().setPort( 5801 ).setPortAutoIncrement( true )
                    .setJoin( new JoinConfig().setMulticastConfig( new MulticastConfig().setEnabled( false ) ) ) );
            config.setSerializationConfig( new SerializationConfig().setSerializerConfigs( ImmutableList.of(
                    new SerializerConfig().setTypeClass( MetricsLogRecord.class ).setImplementation(
                            new MetricsLogRecordStreamSerializer() ),
                    new SerializerConfig().setTypeClass( MetricsUserStatsMetadata.class )
                            .setImplementation( new MetricsUserStatsMetadataStreamSerializer() ) ) ) );
            hazelcast = Hazelcast.newHazelcastInstance( config );
        }

    }

    @AfterClass
    public static final void shutdownHazelcast() {
        hazelcast.shutdown();
        hazelcast = null;
    }

}
