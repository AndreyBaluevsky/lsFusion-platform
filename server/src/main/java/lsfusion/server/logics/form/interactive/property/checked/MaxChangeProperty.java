package lsfusion.server.logics.form.interactive.property.checked;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.form.interactive.instance.filter.NotFilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.NotNullFilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.struct.filter.ContextFilter;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.function.Function;

// св-во которое дает максимальное значение при изменении DataProperty для переданных ключей и значения
public class MaxChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends PullChangeProperty<T, P, MaxChangeProperty.Interface<P>> {

    public abstract static class Interface<P extends PropertyInterface> extends PropertyInterface<Interface<P>> {

        Interface(int ID) {
            super(ID);
        }

        public abstract Expr getExpr();

        public abstract PropertyObjectInterfaceInstance getInterface(ImMap<P, DataObject> mapValues, ObjectInstance valueObject);
    }

    public static class KeyInterface<P extends PropertyInterface> extends Interface<P> {

        P propertyInterface;

        public KeyInterface(P propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.getChangeExpr();
        }

        public PropertyObjectInterfaceInstance getInterface(ImMap<P, DataObject> mapValues, ObjectInstance valueObject) {
            return mapValues.get(propertyInterface);
        }
    }

    public static class ValueInterface<P extends PropertyInterface> extends Interface<P> {

        Property<P> toChange;

        public ValueInterface(Property<P> toChange) {
            super(1000);

            this.toChange = toChange;  
        }

        public Expr getExpr() {
            return toChange.getChangeExpr();
        }

        public PropertyObjectInterfaceInstance getInterface(ImMap<P, DataObject> mapValues, ObjectInstance valueObject) {
            return valueObject;
        }
    }

    public static <P extends PropertyInterface> ImOrderSet<Interface<P>> getInterfaces(Property<P> property) {
        return property.getFriendlyOrderInterfaces().mapOrderSetValues((Function<P, Interface<P>>) KeyInterface::new).addOrderExcl(new ValueInterface<>(property));
    }

    public MaxChangeProperty(Property<T> onChange, Property<P> toChange) {
        super(LocalizedString.concatList(onChange.caption, " по (", toChange.caption, ")"), getInterfaces(toChange), onChange, toChange);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface<P>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(!calcType.isExpr()) // пока так
            calcType = CalcType.EXPR;

        ImMap<Interface<P>, Expr> mapExprs = interfaces.mapValues((Function<Interface<P>, Expr>) Interface::getExpr);

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(onChange.getMapKeys(),
                calcType, toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.LOGICAL(), joinImplement); // constraints (filters)
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }

    public ContextFilter getContextFilter(final ImMap<P, DataObject> mapValues, final ObjectEntity valueObject) {
        return factory -> {
            ImMap<Interface<P>, PropertyObjectInterfaceInstance> interfaceImplement = interfaces.mapValues((Interface<P> value) -> value.getInterface(mapValues, valueObject.getInstance(factory)));
            return new NotFilterInstance(new NotNullFilterInstance<>(
                    new PropertyObjectInstance<>(MaxChangeProperty.this, interfaceImplement)));
        };
    }
}
