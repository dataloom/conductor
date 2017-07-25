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

package com.kryptnostic.conductor.pods;

import com.dataloom.data.mapstores.DataMapstore;
import com.datastax.driver.core.Session;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hazelcast.core.HazelcastInstance;
import com.kryptnostic.conductor.DataLoadingService;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

/**
 * @author Matthew Tamayo-Rios &lt;matthew@openlattice.com&gt;
 */
public class ConductorLoadingPod {
    private static final Logger logger = LoggerFactory.getLogger( ConductorLoadingPod.class );
    @Inject
    private HazelcastInstance hazelcastInstance;

    @Inject
    private DataMapstore dataMapstore;

    @Inject
    private ListeningExecutorService executor;

    @Inject
    private Session session;

    @Bean
    public DataLoadingService dls() {
        return new DataLoadingService( session, hazelcastInstance, executor, dataMapstore );
    }

    @PostConstruct
    public void startLoading() {
        logger.info( "Kicking off background loading" );
        dls().backgroundLoad();
    }

}
