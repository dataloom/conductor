package com.kryptnostic.metrics.v1.userstats;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.inject.Inject;

import org.joda.time.LocalDate;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.kryptnostic.mapstores.v1.constants.HazelcastNames;
import com.kryptnostic.metrics.v1.MetricsLogRecord;
import com.kryptnostic.metrics.v1.MetricsUserStatsMetadata;

public class MetricsUserStatsHandler extends Handler {

    private IMap<LocalDate, Integer>               date_count;
    private IMap<LocalDate, String>                date_users;
    private IMap<String, MetricsUserStatsMetadata> dateuser_data;

    @Inject
    public MetricsUserStatsHandler( HazelcastInstance hazelcastInstance ) {
        date_count = hazelcastInstance.getMap( HazelcastNames.Maps.DATE_COUNT );
        date_users = hazelcastInstance.getMap( HazelcastNames.Maps.DATE_USERS );
        dateuser_data = hazelcastInstance.getMap( HazelcastNames.Maps.DATEUSER_DATA );
    }

    public void publishToDB( MetricsLogRecord record, MetricsUserStatsMetadata metadata  ) {
        LocalDate date = MetricsDate.getDate();
        String dateuser = MetricsDate.getDay( date ) + record.getUUID();
        if(dateuser_data.containsKey( dateuser )) {
            int count = date_count.getOrDefault( date, 0 );
            date_count.put( date, count++ );
        } else {
            dateuser_data.put( dateuser, metadata );
            date_count.put( date, 1 );
            date_users.put( date, record.getUUID() );
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }

    @Override
    public void publish( LogRecord record ) {

    }

}
