package com.kryptnostic.metrics.v1.pods;

import org.joda.time.LocalDate;

import com.kryptnostic.metrics.v1.MetricsLogRecord;
import com.kryptnostic.metrics.v1.MetricsUserStatsMetadata;
import com.kryptnostic.rhizome.mapstores.SelfRegisteringMapStore;
import com.kryptnostic.rhizome.mapstores.SelfRegisteringQueueStore;

public interface MetricsMapStoresPod {

    SelfRegisteringMapStore<LocalDate, Integer> dateCountMapStore();

    SelfRegisteringMapStore<LocalDate, String> dateUsersMapStore();

    SelfRegisteringMapStore<String, MetricsUserStatsMetadata> dateuserDataMapStore();

    SelfRegisteringQueueStore<MetricsLogRecord> logRecordsQueueStore();

}
