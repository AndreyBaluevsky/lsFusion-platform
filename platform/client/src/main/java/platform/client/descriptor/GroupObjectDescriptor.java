package platform.client.descriptor;

import platform.client.logics.ClientGroupObject;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GroupObjectDescriptor implements IdentitySerializable {

    ClientGroupObject client;

    public int getID() {
        //todo:
        return 0;
    }

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:

    }
}
