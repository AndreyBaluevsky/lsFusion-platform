package platform.client.logics;

import platform.client.descriptor.editor.ComponentEditor;
import platform.base.context.ApplicationContext;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.GroupObjectContainerSet;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientClassChooser extends ClientComponent {

    public ClientClassChooser() {
    }

    public ClientObject object;
    public boolean show = true;

    public ClientClassChooser(int ID, ApplicationContext context) {
        super(ID, context);
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, object);
        outStream.writeBoolean(show);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        object = pool.deserializeObject(inStream);
        show = inStream.readBoolean();

    }

    public boolean getShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
        updateDependency(this, "show");
    }

    @Override
    public String getCaption() {
        return "Дерево классов";
    }

    @Override
    public String toString() {
        return getCaption() + " (" + object.toString() + ")";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }

    @Override
    public String getCodeConstructor(String name) {
        return "RegularFilterGroupView " + name + " = createRegularFilterGroup(" + getID() + ")";
    }
    
    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return GroupObjectContainerSet.getClassChooserDefaultConstraints(super.getDefaultConstraints());
    }
}
