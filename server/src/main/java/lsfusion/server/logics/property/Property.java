package lsfusion.server.logics.property;

import lsfusion.base.ListPermutations;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.simple.EmptyOrderMap;
import lsfusion.base.col.implementations.simple.EmptyRevMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.ClassViewType;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.ActionClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.PropertySIDPolicy;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.actions.edit.DefaultChangeActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.property.group.AbstractNode;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChanges;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import static lsfusion.interop.form.ServerResponse.*;

public abstract class Property<T extends PropertyInterface> extends AbstractNode implements ServerIdentitySerializable {
    public static final GetIndex<PropertyInterface> genInterface = new GetIndex<PropertyInterface>() {
        public PropertyInterface getMapValue(int i) {
            return new PropertyInterface(i);
        }};

    public int ID = 0;
    private String sID;
    private String name;
    private String canonicalName;
    private boolean isInner;

    // вот отсюда идут свойства, которые отвечают за логику представлений и подставляются автоматически для PropertyDrawEntity и PropertyDrawView
    public String caption;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    public boolean loggable;
    public LAP logFormProperty;

    public void setFixedCharWidth(int charWidth) {
        minimumCharWidth = charWidth;
        maximumCharWidth = charWidth;
        preferredCharWidth = charWidth;
    }

    public void inheritFixedCharWidth(Property property) {
        minimumCharWidth = property.minimumCharWidth;
        maximumCharWidth = property.maximumCharWidth;
        preferredCharWidth = property.preferredCharWidth;
    }

    private ImageIcon image;
    private String iconPath;

    public void inheritImage(Property property) {
        image = property.image;
        iconPath = property.iconPath;
    }

    public void setImage(String iconPath) {
        this.iconPath = iconPath;
        this.image = new ImageIcon(Property.class.getResource("/images/" + iconPath));
    }

    public KeyStroke editKey;
    public Boolean showEditKey;

    public String regexp;
    public String regexpMessage;
    public Boolean echoSymbols;

    public boolean drawToToolbar;

    public Boolean shouldBeLast;

    public ClassViewType forceViewType;

    public Boolean askConfirm;
    public String askConfirmMessage;

    public String eventID;

    private String mouseBinding;
    private Object keyBindings;
    private Object contextMenuBindings;
    private Object editActions;

    public String toString() {
        String result = caption;
        if(!isInner)
            result = result + " (" + sID + ")";
        return result;
    }

    public boolean isField() {
        return false;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public LP getLogFormProperty() {
        return logFormProperty;
    }

    public void setLogFormProperty(LAP logFormProperty) {
        this.logFormProperty = logFormProperty;
    }

    public Type getType() {
        return getType(defaultPrevSameClasses);
    }

    public Type getType(PrevClasses prevSameClasses) {
        ValueClass valueClass = getValueClass(prevSameClasses);
        return valueClass != null ? valueClass.getType() : null;
    }

    public ValueClass getValueClass() {
        return getValueClass(defaultPrevSameClasses);
    }
    public abstract ValueClass getValueClass(PrevClasses prevSameClasses);

    public ValueClass[] getInterfaceClasses(ImOrderSet<T> listInterfaces) {
        return listInterfaces.mapOrder(getInterfaceClasses(ClassType.ASSERTFULL)).toArray(new ValueClass[listInterfaces.size()]);
    }
    public abstract ImMap<T, ValueClass> getInterfaceClasses(ClassType type);
    
    public static final PrevClasses defaultPrevSameClasses = PrevClasses.SAME;
    public static final PrevClasses constraintPrevSameClasses = PrevClasses.BASE;
    
    public ClassWhere<T> getClassWhere(ClassType type) {
        return getClassWhere(type, defaultPrevSameClasses);
    }
    public abstract ClassWhere<T> getClassWhere(ClassType type, PrevClasses prevSameClasses);

    public boolean check(boolean constraint) {
        return !getClassWhere(ClassType.ASIS, constraint ? constraintPrevSameClasses : defaultPrevSameClasses).isFalse();
    }

    @IdentityLazy
    public boolean cacheIsInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny) { // для всех подряд свойств не имеет смысла
        return isInInterface(interfaceClasses, isAny);
    }

    public boolean isInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        ClassWhere<T> interfaceClassWhere = new ClassWhere<T>(interfaceClasses);
        ClassWhere<T> fullClassWhere = getClassWhere(ClassType.FULL);

        if(isAny)
            return !fullClassWhere.andCompatible(interfaceClassWhere).isFalse();
        else
            return interfaceClassWhere.meansCompatible(fullClassWhere);
    }

    public Property(String sID, String caption, ImOrderSet<T> interfaces) {
        this.setSID(sID);
        this.caption = caption;
        this.interfaces = interfaces.getSet();
        this.orderInterfaces = interfaces;
    }

    public final ImSet<T> interfaces;
    private final ImOrderSet<T> orderInterfaces;
    public ImOrderSet<T> getOrderInterfaces() {
        return orderInterfaces;
    }

    public static Modifier defaultModifier = new Modifier() {
        public PropertyChanges getPropertyChanges() {
            return PropertyChanges.EMPTY;
        }
    };

    @IdentityLazy
    public Type getInterfaceType(T propertyInterface) {
        return getInterfaceClasses(ClassType.ASSERTFULL).get(propertyInterface).getType();
    }

    public String getSID() {
        return sID;
    }

    public void setSID(String sID) {
        this.sID = sID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name, boolean generated) {
        this.name = name;
        this.isInner = generated;
    }

    public boolean cached = false;

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public void setKeyAction(KeyStroke ks, String actionSID) {
        if (keyBindings == null) {
            keyBindings = MapFact.mMap(MapFact.override());
        }
        ((MMap<KeyStroke, String>)keyBindings).add(ks, actionSID);
    }

    public void setContextMenuAction(String actionSID, String caption) {
        if (contextMenuBindings == null || contextMenuBindings instanceof EmptyOrderMap) {
            contextMenuBindings = MapFact.mOrderMap(MapFact.override());
        }
        ((MOrderMap<String, String>)contextMenuBindings).add(actionSID, caption);
    }

    public void setEditAction(String editActionSID, ActionPropertyMapImplement<?, T> editActionImplement) {
        if (editActions == null || editActions instanceof EmptyRevMap) {
            editActions = MapFact.mMap(MapFact.override());
        }
        ((MMap<String, ActionPropertyMapImplement<?, T>>)editActions).add(editActionSID, editActionImplement);
    }

    public String getMouseBinding() {
        return mouseBinding;
    }

    public ImMap<KeyStroke, String> getKeyBindings() {
        return (ImMap<KeyStroke, String>)(keyBindings == null ? MapFact.EMPTY() : keyBindings);
    }

    public ImOrderMap<String, String> getContextMenuBindings() {
        return (ImOrderMap<String, String>)(contextMenuBindings == null ? MapFact.EMPTYORDER() : contextMenuBindings);
    }

    @LongMutable
    private ImMap<String, ActionPropertyMapImplement<?, T>> getEditActions() {
        return (ImMap<String, ActionPropertyMapImplement<?, T>>)(editActions == null ? MapFact.EMPTY() : editActions);
    }

    public ActionPropertyMapImplement<?, T> getEditAction(String editActionSID) {
        return getEditAction(editActionSID, null);
    }

    public ActionPropertyMapImplement<?, T> getEditAction(String editActionSID, CalcProperty filterProperty) {
        ActionPropertyMapImplement<?, T> editAction = getEditActions().get(editActionSID);
        if (editAction != null) {
            return editAction;
        }

        if (GROUP_CHANGE.equals(editActionSID)) {
            //будем определять на уровне PropertyDraw
            assert false;
        } else if (CHANGE_WYS.equals(editActionSID)) {
//            возвращаем дефолт
        }

        return getDefaultEditAction(editActionSID, filterProperty);
    }

    public boolean isChangeWYSOverriden() {
        return getEditActions().containsKey(CHANGE_WYS);
    }

    public boolean isEditObjectActionDefined() {
        if (getEditActions().containsKey(EDIT_OBJECT)) {
            return true;
        }

        ActionPropertyMapImplement<?, T> editObjectAction = getDefaultEditAction(EDIT_OBJECT, null);
        if (editObjectAction != null && editObjectAction.property instanceof DefaultChangeActionProperty) {
            DefaultChangeActionProperty defaultEditAction = (DefaultChangeActionProperty) editObjectAction.property;
            return defaultEditAction.getImplementType() instanceof ObjectType;
        }

        return false;
    }

    public abstract ActionPropertyMapImplement<?, T> getDefaultEditAction(String editActionSID, CalcProperty filterProperty);

    public boolean checkEquals() {
        return this instanceof CalcProperty;
    }

    public ImRevMap<T, T> getIdentityInterfaces() {
        return interfaces.toRevMap();
    }

    // по умолчанию заполняет свойства
    // assert что entity этого свойства
    public void proceedDefaultDraw(PropertyDrawEntity<T> entity, FormEntity<?> form, Version version) {
        if (shouldBeLast != null)
            entity.shouldBeLast = shouldBeLast;
        if (forceViewType != null)
            entity.forceViewType = forceViewType;
        if (askConfirm != null)
            entity.askConfirm = askConfirm;
        if (askConfirmMessage != null)
            entity.askConfirmMessage = askConfirmMessage;
        if (eventID != null)
            entity.eventID = eventID;
        if (drawToToolbar) {
            entity.setDrawToToolbar(true);
        }
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        if (iconPath != null) {
            propertyView.design.iconPath = iconPath;
            propertyView.design.setImage(image);
        }

        if (editKey != null)
            propertyView.editKey = editKey;
        if (showEditKey != null)
            propertyView.showEditKey = showEditKey;
        if (regexp != null)
            propertyView.regexp = regexp;
        if (regexpMessage != null)
            propertyView.regexpMessage = regexpMessage;
        if (echoSymbols != null)
            propertyView.echoSymbols = echoSymbols;

        if(propertyView.getType() instanceof LogicalClass)
            propertyView.editOnSingleClick = Settings.get().getEditLogicalOnSingleClick();
        if(propertyView.getType() instanceof ActionClass)
            propertyView.editOnSingleClick = Settings.get().getEditActionClassOnSingleClick();
    }

    public boolean hasChild(Property prop) {
        return prop.equals(this);
    }

    public boolean hasNFChild(Property prop, Version version) {
        return hasChild(prop);
    }

    public ImOrderSet<Property> getProperties() {
        return SetFact.singletonOrder((Property) this);
    }

    @Override
    public ImList<PropertyClassImplement> getProperties(ImCol<ImSet<ValueClassWrapper>> classLists, boolean anyInInterface, Version version) {
        MList<PropertyClassImplement> mResultList = ListFact.mList();
        for (ImSet<ValueClassWrapper> classes : classLists) {
            if (interfaces.size() == classes.size()) {
                final ImOrderSet<ValueClassWrapper> orderClasses = classes.toOrderSet();
                for (ImOrderSet<T> mapping : new ListPermutations<T>(getOrderInterfaces())) {
                    ImMap<T, AndClassSet> propertyInterface = mapping.mapOrderValues(new GetIndexValue<AndClassSet, T>() {
                        public AndClassSet getMapValue(int i, T value) {
                            return orderClasses.get(i).valueClass.getUpSet();
                        }});
                    if (isInInterface(propertyInterface, anyInInterface)) {
                        mResultList.add(createClassImplement(orderClasses, mapping));
                    }
                }
            }
        }
        return mResultList.immutableList();
    }
    
    protected abstract PropertyClassImplement<T, ?> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<T> mapping);

    public T getInterfaceById(int iID) {
        for (T inter : interfaces) {
            if (inter.getID() == iID) {
                return inter;
            }
        }

        return null;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeUTF(getSID());
        outStream.writeUTF(caption);
        outStream.writeBoolean(isField());

        pool.serializeCollection(outStream, getOrderInterfaces().toJavaList());
        pool.serializeObject(outStream, getParent());
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        //десериализация не нужна, т.к. вместо создания объекта, происходит поиск в BL
    }

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        return groupsList;
    }

    protected boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;
    }

    public void finalizeAroundInit() {
        super.finalizeAroundInit();
        
        editActions = editActions == null ? MapFact.EMPTY() : ((MMap)editActions).immutable();
        keyBindings = keyBindings == null ? MapFact.EMPTY() : ((MMap)keyBindings).immutable();
        contextMenuBindings = contextMenuBindings == null ? MapFact.EMPTYORDER() : ((MOrderMap)contextMenuBindings).immutableOrder();
    }

    public abstract void prereadCaches();

    protected abstract ImCol<Pair<Property<?>, LinkType>> calculateLinks(boolean calcEvents);

    private ImSet<Link> links;
    @ManualLazy
    public ImSet<Link> getLinks(boolean calcEvents) { // чисто для лексикографики
        if(links==null) {
            links = calculateLinks(calcEvents).mapMergeSetValues(new GetValue<Link, Pair<Property<?>, LinkType>>() {
                public Link getMapValue(Pair<Property<?>, LinkType> value) {
                    return new Link(Property.this, value.first, value.second);
                }});
        }
        return links;
    }
    public void dropLinks() {
        links = null;
    }
    public abstract ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events);
    public abstract ImSet<OldProperty> getParseOldDepends(); // именно так, а не через getSessionCalcDepends, так как может использоваться до инициализации логики

    public ImSet<OldProperty> getOldDepends() {
        return getOldDepends(false);
    }
    public ImSet<OldProperty> getOldDepends(boolean events) {
        return getSessionCalcDepends(events).mapSetValues(new GetValue<OldProperty, SessionCalcProperty>() {
            public OldProperty getMapValue(SessionCalcProperty value) {
                return value.getOldProperty();
            }});
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> ImRevMap<T, V> getMapInterfaces(final ImOrderSet<V> list) {
        return getOrderInterfaces().mapOrderRevValues(new GetIndexValue<V, T>() {
            public V getMapValue(int i, T value) {
                return list.get(i);
            }
        });
    }

    public boolean drillDownInNewSession() {
        return false;
    }

    public Property showDep; // assert что не null когда events не isEmpty

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        assert this.canonicalName == null; 
        this.canonicalName = canonicalName;
    }
    
    public void setCanonicalName(String canonicalName, PropertySIDPolicy policy) {
        setCanonicalName(canonicalName);
        setSID(policy.transformCanonicalNameToSID(canonicalName));
    } 
}
