package lsfusion.server.logics.form.stat.struct.export.plain.dbf;

import com.google.common.base.Throwables;
import com.hexiong.jdbf.JDBFException;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportPlainAction;
import lsfusion.server.logics.form.stat.struct.export.plain.ExportPlainWriter;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;

public class ExportDBFAction<O extends ObjectSelector> extends ExportPlainAction<O> {

    public ExportDBFAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, Integer selectTop, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, exportFiles, selectTop, charset != null ? charset : ExternalUtils.defaultDBFCharset);
    }

    @Override
    protected ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException {
        try {
            return new ExportDBFWriter(fieldTypes, charset);
        } catch (JDBFException e) {
            throw Throwables.propagate(e);
        }
    }
}
