package com.kryptnostic.conductor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import javax.inject.Inject;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.kryptnostic.rhizome.pods.hazelcast.SelfRegisteringStreamSerializer;

public class ConductorCallStreamSerializer implements SelfRegisteringStreamSerializer<ConductorCall> {

    private final ConductorSparkApi api;

    @Inject
    public ConductorCallStreamSerializer( ConductorSparkApi api ) {
        this.api = api;
    }

    @Override
    public void write( ObjectDataOutput out, ConductorCall object ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream( baos );
        oo.writeObject( object );
        oo.flush();
        out.writeByteArray( baos.toByteArray() );
    }

    @Override
    public ConductorCall read( ObjectDataInput in ) throws IOException {
        byte[] b = in.readByteArray();
        ObjectInput input = new ObjectInputStream( new ByteArrayInputStream( b ) );
        ConductorCall c;
        try {
            c = (ConductorCall) input.readObject();
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
            return null;
        }
        c.setApi( api );
        return c;
    }

    @Override
    public int getTypeId() {
        return 1;
    }

    @Override
    public void destroy() {

    }

    @Override
    public Class<ConductorCall> getClazz() {
        return ConductorCall.class;
    }

}
