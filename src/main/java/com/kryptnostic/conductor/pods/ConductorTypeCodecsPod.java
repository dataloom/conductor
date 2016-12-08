package com.kryptnostic.conductor.pods;

import java.util.EnumSet;
import java.util.Set;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dataloom.authorization.requests.Permission;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.extras.codecs.enums.EnumNameCodec;
import com.datastax.driver.extras.codecs.joda.LocalDateCodec;
import com.datastax.driver.extras.codecs.joda.LocalTimeCodec;
import com.kryptnostic.conductor.codecs.EnumSetTypeCodec;
import com.kryptnostic.conductor.codecs.FullQualifiedNameTypeCodec;
import com.kryptnostic.conductor.codecs.TimestampDateTimeTypeCodec;

@Configuration
public class ConductorTypeCodecsPod {
    @Bean
    public TypeCodec<Set<String>> setStringCodec() {
        return TypeCodec.set( TypeCodec.varchar() );
    }

    @Bean
    public FullQualifiedNameTypeCodec fullQualifiedNameTypeCodec() {
        return new FullQualifiedNameTypeCodec();
    }

    @Bean
    public TimestampDateTimeTypeCodec timestampDateTimeTypeCodec() {
        return TimestampDateTimeTypeCodec.getInstance();
    }

    @Bean
    public LocalDateCodec jodaLocalDateCodec() {
        return LocalDateCodec.instance;
    }

    @Bean
    public LocalTimeCodec jodaLocalTimeCodec() {
        return LocalTimeCodec.instance;
    }

    @Bean
    public TypeCodec<EdmPrimitiveTypeKind> edmPrimitiveTypeKindTypeCodec() {
        return new EnumNameCodec<EdmPrimitiveTypeKind>( EdmPrimitiveTypeKind.class );
    }

    @Bean
    public EnumNameCodec<Permission> permissionCodec() {
        return new EnumNameCodec<>( Permission.class );
    }

    @Bean
    public TypeCodec<EnumSet<Permission>> enumSetPermissionCodec() {
        return new EnumSetTypeCodec<Permission>( permissionCodec() );
    }
}
