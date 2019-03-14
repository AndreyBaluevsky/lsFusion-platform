package lsfusion.server.physics.admin.reflection;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.classes.StringClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.infer.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.logics.action.session.change.PropertyChanges;

public class CanonicalNameProperty extends FormulaProperty<CanonicalNameProperty.Interface> {
    private final LAP property;
    private StringClass valueClass = StringClass.get(false, 200);

    public CanonicalNameProperty(LAP property) {
        super(LocalizedString.create("canonical name", false), SetFact.<CanonicalNameProperty.Interface>EMPTYORDER());
        this.property = property;
    }

    @Override
    protected Expr calculateExpr(ImMap joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return property == null ? null : valueClass.getStaticExpr(LocalizedString.create(property.property.getCanonicalName(), false));
    }

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }
}