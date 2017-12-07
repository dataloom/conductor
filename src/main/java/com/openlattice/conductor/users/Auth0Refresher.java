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

package com.openlattice.conductor.users;

import com.codahale.metrics.annotation.Timed;
import com.dataloom.client.RetrofitFactory;
import com.dataloom.directory.pojo.Auth0UserBasic;
import com.dataloom.hazelcast.HazelcastMap;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.kryptnostic.datastore.services.Auth0ManagementApi;
import com.openlattice.authorization.mapstores.UserMapstore;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import retrofit2.Retrofit;

/**
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public class Auth0Refresher {
    public static final  int    REFRESH_INTERVAL_MILLIS = 30000;
    private static final Logger logger                  = LoggerFactory.getLogger( Auth0Refresher.class );
    private static final int    DEFAULT_PAGE_SIZE       = 100;

    private final IMap<String, Auth0UserBasic> users;
    private final Retrofit                     retrofit;
    private final Auth0ManagementApi           auth0ManagementApi;
    private final ILock                        refreshLock;
    private final IAtomicLong                  nextTime;

    public Auth0Refresher( HazelcastInstance hazelcastInstance, String token ) {
        this.users = hazelcastInstance.getMap( HazelcastMap.USERS.name() );
        this.refreshLock = hazelcastInstance.getLock( Auth0Refresher.class.getCanonicalName() );
        this.nextTime = hazelcastInstance.getAtomicLong( UserMapstore.class.getCanonicalName() );
        this.retrofit = RetrofitFactory.newClient( "https://openlattice.auth0.com/api/v2/", () -> token );
        this.auth0ManagementApi = retrofit.create( Auth0ManagementApi.class );
    }

    @Timed
    void refreshAuth0Users() {
        //Only one instance can populate and refresh the map.
        logger.info( "Trying to acquire lock to refresh auth0 users." );
        if ( refreshLock.tryLock() && ( nextTime.get() < System.currentTimeMillis() ) ) {
            logger.info( "Refreshing user list from Auth0." );
            try {
                int page = 0;
                Set<Auth0UserBasic> pageOfUsers = auth0ManagementApi.getAllUsers( page++, DEFAULT_PAGE_SIZE );
                while ( pageOfUsers != null && !pageOfUsers.isEmpty() ) {
                    logger.info( "Loading page {} of {} auth0 users", page, pageOfUsers.size() );
                    for ( Auth0UserBasic user : pageOfUsers ) {
                        users.putTransient( user.getUserId(), user, -1, TimeUnit.MINUTES );
                    }
                    pageOfUsers = auth0ManagementApi.getAllUsers( page++, DEFAULT_PAGE_SIZE );
                }
            } finally {
                logger.info( "Scheduling next refresh." );
                nextTime.set( System.currentTimeMillis() + REFRESH_INTERVAL_MILLIS );
                refreshLock.unlock();
            }
        }
    }

    public static class Auth0RefreshDriver {
        private final Auth0Refresher refresher;

        public Auth0RefreshDriver( Auth0Refresher refresher ) {
            this.refresher = refresher;
        }

        @Scheduled( fixedRate = REFRESH_INTERVAL_MILLIS )
        void refreshAuth0Users() {
            refresher.refreshAuth0Users();
        }
    }
}
