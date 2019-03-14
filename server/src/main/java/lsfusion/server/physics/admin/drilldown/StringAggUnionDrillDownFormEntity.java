package lsfusion.server.physics.admin.drilldown;

import lsfusion.server.data.expr.StringAggUnionProperty;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class StringAggUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<StringAggUnionProperty.Interface, StringAggUnionProperty> {

    public StringAggUnionDrillDownFormEntity(String sID, LocalizedString caption, StringAggUnionProperty property, LogicsModule LM) {
        super(sID, caption, property, LM);
    }
}