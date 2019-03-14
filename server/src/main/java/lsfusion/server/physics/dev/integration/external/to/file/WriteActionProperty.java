package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.WriteClientAction;
import lsfusion.base.file.WriteUtils;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.classes.FileClass;
import lsfusion.server.logics.classes.StaticFormatFileClass;
import lsfusion.server.logics.classes.StringClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.DataObject;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;

public class WriteActionProperty extends SystemExplicitAction {
    private final Type sourcePropertyType;
    private boolean clientAction;
    private boolean dialog;
    private boolean append;

    public WriteActionProperty(Type sourcePropertyType, boolean clientAction, boolean dialog, boolean append, ValueClass sourceProp, ValueClass pathProp) {
        super(sourceProp, pathProp);
        this.sourcePropertyType = sourcePropertyType;
        this.clientAction = clientAction;
        this.dialog = dialog;
        this.append = append;
    }

    @Override
    protected boolean allowNulls() {
        return false;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject sourceObject = context.getDataKeys().getValue(0);
        assert sourceObject.getType() instanceof FileClass;

        DataObject pathObject = context.getDataKeys().getValue(1);
        assert pathObject.getType() instanceof StringClass;
        String path = (String) pathObject.object;

        String extension = null;
        RawFileData rawFileData = null;
        if (sourceObject.object != null) {
            if (sourcePropertyType instanceof StaticFormatFileClass) {
                rawFileData = (RawFileData) sourceObject.object;
                extension = ((StaticFormatFileClass) sourcePropertyType).getOpenExtension(rawFileData);
            } else {
                extension = ((FileData) sourceObject.object).getExtension();
                rawFileData = ((FileData) sourceObject.object).getRawFile();
            }
        }
        try {
            if (rawFileData != null) {
                if (clientAction) {
                    if(append && dialog) {
                        throw new RuntimeException("APPEND is not supported in WRITE CLIENT DIALOG");
                    } else {
                        context.delayUserInteraction(new WriteClientAction(rawFileData, path, extension, append, dialog));
                    }
                } else {
                    WriteUtils.write(rawFileData, path, extension, append);
                }
            } else {
                throw new RuntimeException("File bytes not specified");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}