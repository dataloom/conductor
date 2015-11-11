package com.kryptnostic.metrics.v1;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.heracles.directory.v1.objects.Emails;
import com.kryptnostic.heracles.directory.v1.objects.UserProxy;
import com.kryptnostic.heracles.directory.v1.services.DirectoryService;
import com.kryptnostic.instrumentation.v1.constants.InstrumentationConstants;
import com.kryptnostic.instrumentation.v1.constants.MetricsField;
import com.kryptnostic.instrumentation.v1.constants.MetricsIndex;
import com.kryptnostic.instrumentation.v1.constants.MetricsType;
import com.kryptnostic.metrics.v1.userstats.MetricsUserStatsHandler;

public class MetricsLogger {
    private MetricsUserStatsHandler     mush;
    private MetricsElasticsearchHandler meh;
    private DirectoryService            directoryService;

    private static final Logger         logger = LoggerFactory.getLogger( MetricsLogger.class );

    public MetricsLogger(
            MetricsElasticsearchHandler meh,
            MetricsUserStatsHandler mush,
            DirectoryService directoryService ) {
        this.meh = meh;
        this.mush = mush;
        this.directoryService = directoryService;
    }

    @Inject
    public MetricsLogger(
            HazelcastInstance hazelcastInstance,
            DirectoryService directoryService,
            MetricsElasticsearchHandler meh,
            MetricsUserStatsHandler mush ) {
        this.directoryService = directoryService;
        this.meh = meh;
        this.mush = mush;
        meh.setFormatter( new MetricsLogFormatter() );
        mush.setFormatter( new MetricsLogFormatter() );
    }

    public void logMetrics( String message, Map<MetricsField, Object> map, MetricsIndex index, MetricsType type ) {
        Optional<String> domain = Optional.absent();
        Optional<Emails> emails = Optional.absent();
        String uuidString = map.get( MetricsField.USER_UUID_FIELD ).toString();
        if ( !uuidString.equals( InstrumentationConstants.ANONYMOUS_USER ) ) {
            UUID uuid = UUID.fromString( uuidString );
            UserProxy proxy = new UserProxy( uuid, directoryService );
            emails = Optional.fromNullable( directoryService.getEmails( uuid ) );
            if ( emails.isPresent() ) {
                domain = Optional.fromNullable( proxy.getDomain() );
                String username = proxy.getName();
                map.put( MetricsField.EMAIL_FIELD, emails.get() );
                map.put( MetricsField.USER_NAME_FIELD, username );
                if ( domain.isPresent() ) {
                    map.put( MetricsField.DOMAIN_FIELD, domain.get() );
                }
            } else {
                logger.error( "user is corrupted; unable to load user from directory service" );
            }
            MetricsLogRecord record = new MetricsLogRecord( message, map, index, type, uuidString );
            meh.publishToElasticsearch( record );
            if ( domain.isPresent() ) {
                MetricsUserStatsMetadata metadata = new MetricsUserStatsMetadata( domain.get() );
                mush.publishToDB( record, metadata );
            }
        }
    }
}
