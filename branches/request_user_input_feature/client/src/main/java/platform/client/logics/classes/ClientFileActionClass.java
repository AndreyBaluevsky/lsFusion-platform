package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientFileActionClass extends ClientActionClass {

    private boolean multiple;
    private boolean custom;
    private String filterDescription;
    private String filterExtensions[];

    private String sID;

    @Override
    public String getSID() {
        return sID;
    }

    public ClientFileActionClass(DataInputStream inStream) throws IOException {
        super(inStream);

        multiple = inStream.readBoolean();
        custom = inStream.readBoolean();
        filterDescription = inStream.readUTF();
        int extCount = inStream.readInt();
        if (extCount <= 0) {
            filterExtensions = new String[1];
            filterExtensions[0] = "*";
        } else {
            filterExtensions = new String[extCount];

            for (int i = 0; i < extCount; ++i) {
                filterExtensions[i] = inStream.readUTF();
            }
        }
        sID = "FileActionClass[" + filterDescription + "," + filterExtensions + "]";
    }

    @Override
    public byte getTypeId() {
        return Data.FILEACTION;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        //todo:
    }

    @Override
    public PropertyEditorComponent getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) {
        return custom ? new FilePropertyEditor(multiple) : new FilePropertyEditor(multiple, filterDescription, filterExtensions);
    }
}