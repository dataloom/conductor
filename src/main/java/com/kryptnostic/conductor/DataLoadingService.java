/*
 * Copyright (C) 2017. OpenLattice, Inc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the owner of the copyright at support@openlattice.com
 *
 */

package com.kryptnostic.conductor;

import com.dataloom.data.EntityKey;
import com.dataloom.data.mapstores.DataMapstore;
import com.dataloom.data.requests.Entity;
import com.dataloom.hazelcast.HazelcastMap;
import com.dataloom.streams.StreamUtil;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.kryptnostic.datastore.cassandra.RowAdapters;

/**
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public class DataLoadingService {
    private final ListeningExecutorService executor;
    private final DataMapstore             dataMapstore;
    private final IMap<EntityKey, Entity>  data;
    private final Session                  session;

    public DataLoadingService(
            Session session,
            HazelcastInstance hz,
            ListeningExecutorService executor,
            DataMapstore dataMapstore ) {
        this.executor = executor;
        this.dataMapstore = dataMapstore;
        this.session = session;
        this.data = hz.getMap( HazelcastMap.DATA.name() );
    }

    public void backgroundLoad() {
        executor.execute( () ->
                StreamUtil.stream( session
                        .execute( DataMapstore.currentSyncs( session ) ) )
                        .parallel()
                        .map( dataMapstore::getEntityKeys )
                        .map( ResultSetFuture::getUninterruptibly )
                        .flatMap( StreamUtil::stream )
                        .map( RowAdapters::entityKeyFromData )
                        .forEach( data::get ) );

        //.unordered();
        //.distinct()::iterator;
    }
}
