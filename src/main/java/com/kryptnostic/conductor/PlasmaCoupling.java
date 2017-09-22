/*
 * Copyright (C) 2017. Kryptnostic, Inc (dba Loom)
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
 * You can contact the owner of the copyright at support@thedataloom.com
 */

package com.kryptnostic.conductor;

import com.dataloom.hazelcast.serializers.BlockingAggregatorStreamSerializer;
import com.dataloom.hazelcast.serializers.ConductorElasticsearchCallStreamSerializer;
import com.dataloom.hazelcast.serializers.MatchingAggregatorStreamSerializer;
import com.kryptnostic.conductor.rpc.ConductorElasticsearchApi;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Configuration
public class PlasmaCoupling {
    @Inject
    private ConductorElasticsearchApi elasticsearchApi;

    @Inject
    private ConductorElasticsearchCallStreamSerializer cecss;

    @Inject
    private MatchingAggregatorStreamSerializer mass;

    @Inject
    private BlockingAggregatorStreamSerializer bass;

    @PostConstruct
    public void connect() {
        cecss.setConductorElasticsearchApi( elasticsearchApi );
        mass.setConductorElasticsearchApi( elasticsearchApi );
        bass.setConductorElasticsearchApi( elasticsearchApi );
    }
}
