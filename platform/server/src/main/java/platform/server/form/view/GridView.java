package platform.server.form.view;

import platform.interop.form.layout.GroupObjectContainerSet;
import platform.interop.form.layout.SimplexConstraints;
import platform.server.serialization.ServerSerializationPool;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GridView extends ComponentView {

    public boolean showFind = false;
    public boolean showFilter = true;

    public byte minRowCount = 0;
    public boolean tabVertical = false;
    public boolean autoHide = false;

    public GroupObjectView groupObject;

    public GridView() {
        
    }
    public GridView(int ID, GroupObjectView groupObject) {
        super(ID);

        this.groupObject = groupObject;
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return GroupObjectContainerSet.getGridDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        outStream.writeBoolean(showFind);
        outStream.writeBoolean(showFilter);

        outStream.writeByte(minRowCount);
        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        showFind = inStream.readBoolean();
        showFilter = inStream.readBoolean();

        minRowCount = inStream.readByte();
        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();

        groupObject = pool.deserializeObject(inStream);
    }

}
