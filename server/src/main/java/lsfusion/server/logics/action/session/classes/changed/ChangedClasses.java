package lsfusion.server.logics.action.session.classes.changed;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.session.classes.change.ClassChanges;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;

import java.util.Map;

// аналог changedClasses в ClassChanges только immutable
public class ChangedClasses {
    public final ImMap<ClassDataProperty, ChangedDataClasses> data;
    
    public ChangedClasses(ImMap<ClassDataProperty, ChangedDataClasses> data) {
        this.data = data;
    }

    public ImSet<Property> getChangedProps(BaseClass baseClass) {
        return ClassChanges.getChangedProps(data, baseClass);
    }

    public static ImSet<CustomClass> getAllRemoveClasses(Map<ClassDataProperty, ChangedDataClasses> data) {
        ImSet<CustomClass> result = SetFact.EMPTY();
        for(ChangedDataClasses dataChanged : data.values())
            result = result.merge(dataChanged.remove); // так быстрее так как как правило одно изменение
        return result;
    }
    public ChangedDataClasses getAll() {
        ChangedDataClasses result = ChangedDataClasses.EMPTY;
        for(ChangedDataClasses dataChanged : data.valueIt())
            result = result.merge(dataChanged);
        return result;
    }
}
