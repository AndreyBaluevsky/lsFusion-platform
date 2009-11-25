package platform.server.view.form;

import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.ChangesSession;

import java.sql.SQLException;
import java.util.Collection;

// ObjectImplement table'ы
public class DataObjectImplement extends ObjectImplement {

    DataClass dataClass;
    Object value;

    public DataObjectImplement(int iID, String sID, DataClass iDataClass, String iCaption) {
        super(iID,sID,iCaption);
        dataClass = iDataClass;
        value = dataClass.getDefaultValue();
    }

    public AndClassSet getClassSet(GroupObjectImplement classGroup) {
        return dataClass;
    }

    public ValueClass getGridClass() {
        return dataClass;
    }

    public DataClass getBaseClass() {
        return dataClass;
    }

    public void changeValue(ChangesSession session, Object changeValue) throws SQLException {
        if(changeValue==null) changeValue = dataClass.getDefaultValue();
        if(value.equals(changeValue)) return;

        value = changeValue;

        updated = updated | UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectImplement.UPDATED_OBJECT;
    }

    public void changeValue(ChangesSession session, ObjectValue changeValue) throws SQLException {
        changeValue(session, changeValue.getValue());
    }

    public boolean classChanged(Collection<CustomClass> changedClasses) {
        return false;
    }

    // собсно null быть не может (пока) поэтому проверки как в CustomObjectImplement и не надо (как и UPDATED_CLASS) 
    public boolean classUpdated(GroupObjectImplement classGroup) {
        return false;
    }
    public boolean isInInterface(GroupObjectImplement group) {
        return true;
    }

    public ObjectValue getObjectValue() {
        return DataObject.getValue(value,dataClass);
    }

    protected Expr getExpr() {
        return new ValueExpr(value,dataClass);
    }

    public Type getType() {
        return dataClass;
    }
}
