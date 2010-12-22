package platform.server.data;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Field {
    public String name;
    public Type type;

    protected Field(String name,Type type) {
        this.name = name;
        this.type = type;
    }

    public String getDeclare(SQLSyntax syntax) {
        return name + " " + type.getDB(syntax);
    }

    public String toString() {
        return name;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
        outStream.writeUTF(name);
        TypeSerializer.serializeType(outStream,type);
    }

    protected Field(DataInputStream inStream) throws IOException {
        name = inStream.readUTF();
        type = TypeSerializer.deserializeType(inStream);
    }

    public static Field deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readByte();
        if(type==0) return new KeyField(inStream);
        if(type==1) return new PropertyField(inStream);

        throw new IOException();
    }

    abstract byte getType();

    @Override
    public boolean equals(Object obj) {
        return this==obj || getClass()==obj.getClass() && name.equals(((Field)obj).name) && type.equals(((Field)obj).type);
    }

    @Override
    public int hashCode() {
        return (getClass().hashCode() * 31 + name.hashCode()) * 31 + type.hashCode();
    }
}
