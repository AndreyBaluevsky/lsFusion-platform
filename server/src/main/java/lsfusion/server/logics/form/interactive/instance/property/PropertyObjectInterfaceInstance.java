package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;

public interface PropertyObjectInterfaceInstance extends OrderInstance {

    AndClassSet getClassSet(ImSet<GroupObjectInstance> gridGroups);

    DataObject getDataObject();
    ObjectValue getObjectValue();

    boolean isNull();

    ConcreteClass getCurrentClass();
}
