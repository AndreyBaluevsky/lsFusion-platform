package lsfusion.server.logics.form.struct.property;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.ExplicitAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.authentication.security.policy.ViewPropertySecurityPolicy;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.interop.action.ServerResponse.*;
import static lsfusion.server.logics.form.struct.property.PropertyDrawExtraType.*;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, PropertyReaderEntity {

    private PropertyEditType editType = PropertyEditType.EDITABLE;
    
    private final ActionOrPropertyObjectEntity<P, ?> propertyObject;
    
    public GroupObjectEntity toDraw;

    private String mouseBinding;
    private Map<KeyStroke, String> keyBindings;
    private OrderedMap<String, LocalizedString> contextMenuBindings;
    private Map<String, ActionObjectEntity<?>> editActions;

    public boolean optimisticAsync;

    public Boolean askConfirm;
    public String askConfirmMessage;

    public Boolean shouldBeLast;
    public ClassViewType forceViewType; // assert not null, after initialization
    public String eventID;

    private String formPath;

    private Pair<Integer, Integer> scriptIndex;
    
    public LocalizedString initCaption = null; // чисто техническая особенность реализации
    
    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public String columnsName;
    public Object columnGroupObjects = SetFact.mOrderExclSet();
    private boolean finalizedColumnGroupObjects;

    private Map<PropertyDrawExtraType, PropertyObjectEntity<?>> propertyExtras = new HashMap<>();

    public Group group;
    
    public Group getGroup() {
        return (group != null ? (group == Group.NOGROUP ? null : group) : getInheritedProperty().getParent());
    }
    public Group getNFGroup(Version version) {
        return (group != null ? (group == Group.NOGROUP ? null : group) : getInheritedProperty().getNFParent(version));
    }

    public boolean attr;

    // for pivoting
    public String formula;
    public ImList<PropertyDrawEntity> formulaOperands;

    public PropertyGroupType aggrFunc;
    public ImList<PropertyObjectEntity> lastAggrColumns = ListFact.EMPTY();
    public boolean lastAggrDesc;

    public PropertyDrawEntity quickFilterProperty;

    public void fillQueryProps(MExclSet<PropertyReaderEntity> mResult) {
        mResult.exclAdd(this);
        PropertyDrawExtraType[] propTypes = {CAPTION, FOOTER, SHOWIF, BACKGROUND, FOREGROUND}; // READONLYIF is absent here
        for (PropertyDrawExtraType type : propTypes) {
            fillQueryProp(mResult, getPropertyExtra(type), extraReaders.get(type));
        }
    }

    private void fillQueryProp(MExclSet<PropertyReaderEntity> mResult, PropertyObjectEntity<?> property, PropertyReaderEntity reader) {
        if (property != null) {
            mResult.exclAdd(reader);
        }
    }

    public class PropertyDrawReader implements PropertyReaderEntity {
        private PropertyDrawExtraType type;
        
        public PropertyDrawReader(PropertyDrawExtraType type) {
            this.type = type;    
        }
        
        @Override
        public Type getType() {
            return getPropertyObjectEntity().getType();
        }

        @Override
        public String getReportSID() {
            return PropertyDrawEntity.this.getReportSID() + getReportSuffix();
        }

        @Override
        public PropertyObjectEntity getPropertyObjectEntity() {
            return propertyExtras.get(type);
        }

        @Override
        public int getID() {
            return PropertyDrawEntity.this.getID();
        }

        @Override
        public String getSID() {
            return PropertyDrawEntity.this.getSID();
        }

        @Override
        public Object getProfiledObject() {
            return getPropertyObjectEntity();
        }

        @Override
        public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
            return PropertyDrawEntity.this.getColumnGroupObjects();
        }
        
        @Override
        public String toString() {
            return ThreadLocalContext.localize(type.getText()) + "(" + PropertyDrawEntity.this.toString() + ")";            
        }
        
        protected String getReportSuffix() {
            return type.getReportExtraType().getReportFieldNameSuffix();
        }
    }

    public final Map<PropertyDrawExtraType, PropertyDrawReader> extraReaders;
    
    private ActionOrProperty inheritedProperty;

    public PropertyDrawEntity(int ID, String sID, ActionOrPropertyObjectEntity<P, ?> propertyObject, ActionOrProperty inheritedProperty) {
        super(ID);
        setSID(sID);
        setIntegrationSID(sID);
        this.propertyObject = propertyObject;
        this.inheritedProperty = inheritedProperty;
        
        this.extraReaders = new HashMap<>();
        PropertyDrawExtraType[] extraTypes = {CAPTION, FOOTER, SHOWIF, BACKGROUND, FOREGROUND};
        for (PropertyDrawExtraType type : extraTypes) {
            this.extraReaders.put(type, new PropertyDrawReader(type));
        }
    }

    public DataClass getRequestInputType(ImSet<SecurityPolicy> policies) {
        return getRequestInputType(CHANGE, policies, optimisticAsync);
    }

    public DataClass getWYSRequestInputType(ImSet<SecurityPolicy> policies) {
        return getRequestInputType(CHANGE_WYS, policies, true); // wys is optimistic by default
    }
    
    public boolean isProperty() {
        return getValueActionOrProperty() instanceof PropertyObjectEntity;
    }

    public OrderEntity<?> getOrder() {
        return getValueProperty();
    }

    public DataClass getRequestInputType(String actionSID, ImSet<SecurityPolicy> securityPolicies, boolean optimistic) {
        if (isProperty()) { // optimization
            ActionObjectEntity<?> changeAction = getEditAction(actionSID, securityPolicies);

            if (changeAction != null) {
                return (DataClass)changeAction.property.getSimpleRequestInputType(optimistic);
            }
        }
        return null;
    }

    public <A extends PropertyInterface> Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form, ImSet<SecurityPolicy> policies) {
        ActionObjectEntity<A> changeAction = (ActionObjectEntity<A>) getEditAction(CHANGE, policies);
        if(changeAction!=null)
            return changeAction.getAddRemove(form);
        return null;
    }

    private boolean isEdit(String editActionSID) {
        // GROUP_CHANGE can also be in context menu binding (see Property constructor)
        boolean isEdit = CHANGE.equals(editActionSID) || CHANGE_WYS.equals(editActionSID) || EDIT_OBJECT.equals(editActionSID) || GROUP_CHANGE.equals(editActionSID);
        assert isEdit || hasContextMenuBinding(editActionSID) || hasKeyBinding(editActionSID);
        return isEdit;
    }
    
    private boolean checkPermission(Action editAction, String editActionSID, SQLCallable<Boolean> checkReadOnly, ImSet<SecurityPolicy> securityPolicies) throws SQLException, SQLHandledException {
        ActionOrProperty securityProperty;
        if (isEdit(editActionSID) && !editAction.ignoreReadOnlyPolicy()) { // if event handler doesn't change anything (for example SELECTOR), consider this event to be binding (not edit) 
            if (isReadOnly() || (checkReadOnly != null && checkReadOnly.call())) 
                return false;

            securityProperty = getSecurityProperty(); // will check property itself 
        } else { // menu or key binding
            securityProperty = editAction;
        }

        for(SecurityPolicy securityPolicy : securityPolicies)
            if(forbidEditObjects(editActionSID, securityPolicy))
                return false;
        return SecurityPolicy.checkPropertyChangePermission(securityPolicies, securityProperty);
    }

    private boolean forbidEditObjects(String editActionSID, SecurityPolicy securityPolicy) {
        return EDIT_OBJECT.equals(editActionSID) && (securityPolicy.editObjects == null || !securityPolicy.editObjects);
    }

    public ActionObjectEntity<?> getEditAction(String actionId, ImSet<SecurityPolicy> securityPolicies) {
        try {
            return getEditAction(actionId, null, securityPolicies);
        } catch (SQLException | SQLHandledException e) {
            assert false;
            throw Throwables.propagate(e);
        }
    }
    
    public ActionObjectEntity<?> getEditAction(String actionId, SQLCallable<Boolean> checkReadOnly, ImSet<SecurityPolicy> securityPolicies) throws SQLException, SQLHandledException {
        ActionObjectEntity<?> editAction = getEditAction(actionId);

        if (editAction != null && !checkPermission(editAction.property, actionId, checkReadOnly, securityPolicies))
            return null;
        
        return editAction;
    }

    public ActionObjectEntity<?> getEditAction(String actionId) {
        if (editActions != null) {
            ActionObjectEntity editAction = editActions.get(actionId);
            if (editAction != null)
                return editAction;
        }

        ActionOrProperty<P> editProperty = getEditProperty();
        ActionMapImplement<?, P> editActionImplement = editProperty.getEditAction(actionId);
        if(editActionImplement != null)
            return editActionImplement.mapObjects(getEditMapping());

        // default implementations for group change and change wys
        if (GROUP_CHANGE.equals(actionId) || CHANGE_WYS.equals(actionId)) {
            ActionObjectEntity<?> editAction = getEditAction(CHANGE);
            if (editAction != null) {
                if (GROUP_CHANGE.equals(actionId)) // if there is no group change, then generate one
                    return editAction.getGroupChange();
                else { // if CHANGE action requests DataClass, then use this action
                    assert CHANGE_WYS.equals(actionId);
                    if (editAction.property.getSimpleRequestInputType(true) != null) // wys is optimistic by default
                        return editAction;
                    else {
                        ActionMapImplement<?, P> defaultWYSAction = editProperty.getDefaultWYSAction();
                        if(defaultWYSAction != null) // assert getSimpleRequestInputType != null
                            return defaultWYSAction.mapObjects(getEditMapping());
                    }
                }
            }
        }
        return null;
    }

    public ActionObjectEntity<?> getSelectorAction(FormEntity entity, Version version) {
        GroupObjectEntity groupObject = getNFToDraw(entity, version);
        if(groupObject != null) {
            for (ObjectEntity objectInstance : getObjectInstances().filter(groupObject.getObjects())) {
                if (objectInstance.baseClass instanceof CustomClass) {
                    ExplicitAction dialogAction = objectInstance.getChangeAction();
                    return new ActionObjectEntity<>(dialogAction, MapFact.singletonRev(dialogAction.interfaces.single(), objectInstance));
                }
            }
        }
        return null;
    }

    public PropertyDrawInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public void setKeyAction(KeyStroke ks, String actionSID) {
        if (keyBindings == null) {
            keyBindings = new HashMap<>();
        }
        keyBindings.put(ks, actionSID);
    }

    public void setContextMenuAction(String actionSID, LocalizedString caption) {
        if (contextMenuBindings == null) {
            contextMenuBindings = new OrderedMap<>();
        }
        contextMenuBindings.put(actionSID, caption);
    }

    public void setEditAction(String actionSID, ActionObjectEntity<?> editAction) {
        if(editActions==null) {
            editActions = new HashMap<>();
        }
        editActions.put(actionSID, editAction);
    }

    private ActionOrProperty<P> getEditProperty() {
        return propertyObject.property;
    }     
    private ImRevMap<P, ObjectEntity> getEditMapping() {
        return propertyObject.mapping;
    }     
    
    public OrderedMap<String, LocalizedString> getContextMenuBindings() {
        ImOrderMap<String, LocalizedString> propertyContextMenuBindings = getEditProperty().getContextMenuBindings(); 
        if (propertyContextMenuBindings.isEmpty()) {
            return contextMenuBindings;
        }

        OrderedMap<String, LocalizedString> result = new OrderedMap<>();
        for (int i = 0; i < propertyContextMenuBindings.size(); ++i) {
            result.put(propertyContextMenuBindings.getKey(i), propertyContextMenuBindings.getValue(i));
        }

        if (contextMenuBindings != null) {
            result.putAll(contextMenuBindings);
        }

        return result;
    }

    public boolean hasContextMenuBinding(String actionSid) {
        OrderedMap contextMenuBindings = getContextMenuBindings();
        return contextMenuBindings != null && contextMenuBindings.containsKey(actionSid);
    }
    
    public boolean hasKeyBinding(String actionId) {
        Map keyBindings = getKeyBindings();
        return keyBindings != null && keyBindings.containsValue(actionId);
    }

    public Map<KeyStroke, String> getKeyBindings() {
        ImMap<KeyStroke, String> propertyKeyBindings = getEditProperty().getKeyBindings();
        if (propertyKeyBindings.isEmpty()) {
            return keyBindings;
        }

        Map<KeyStroke, String> result = propertyKeyBindings.toJavaMap();
        if (keyBindings != null) {
            result.putAll(keyBindings);
        }
        return result;
    }

    public String getMouseBinding() {
        return mouseBinding != null ? mouseBinding : getEditProperty().getMouseBinding();
    }

    @LongMutable
    public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
        if(!finalizedColumnGroupObjects) {
            finalizedColumnGroupObjects = true;
            columnGroupObjects = ((MOrderExclSet<GroupObjectEntity>)columnGroupObjects).immutableOrder();
        }

        return (ImOrderSet<GroupObjectEntity>)columnGroupObjects;
    }
    public void setColumnGroupObjects(String columnsName, ImOrderSet<GroupObjectEntity> columnGroupObjects) {
        assert !finalizedColumnGroupObjects;
        this.columnsName = columnsName;
        finalizedColumnGroupObjects = true;
        this.columnGroupObjects = columnGroupObjects;
    }

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject) {
        assert !finalizedColumnGroupObjects;
        ((MOrderExclSet<GroupObjectEntity>)columnGroupObjects).exclAdd(columnGroupObject);
    }

    public void setPropertyExtra(PropertyObjectEntity<?> property, PropertyDrawExtraType type) {
        propertyExtras.put(type, property);
    }
    
    public PropertyObjectEntity<?> getPropertyExtra(PropertyDrawExtraType type) {
        return propertyExtras.get(type);
    }
    
    public boolean hasPropertyExtra(PropertyDrawExtraType type) {
        return propertyExtras.get(type) != null;
    }
    
    public PropertyEditType getEditType() {
        return editType;
    }

    public void setEditType(PropertyEditType editType) {
        this.editType = editType;
    }

    public boolean isReadOnly() {
        return editType == PropertyEditType.READONLY;
    }

    public boolean isEditable() {
        return editType == PropertyEditType.EDITABLE;
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView defaultView) {
        getInheritedProperty().drawOptions.proceedDefaultDesign(propertyView);
    }

    public void proceedDefaultDraw(FormEntity form) {
        getInheritedProperty().drawOptions.proceedDefaultDraw(this, form);
    }

    @Override
    public String toString() {
        return (formPath == null ? "" : formPath) + " property:" + propertyObject.toString();
    }

    // interactive
    public GroupObjectEntity getToDraw(FormEntity form) {
        return toDraw==null? getApplyObject(form, SetFact.EMPTY(), true) :toDraw;
    }

    public GroupObjectEntity getApplyObject(FormEntity form, ImSet<GroupObjectEntity> excludeGroupObjects, boolean supportGroupColumns) {
        if(supportGroupColumns)
            excludeGroupObjects = excludeGroupObjects.merge(getColumnGroupObjects().getSet());
        return form.getApplyObject(getObjectInstances(), excludeGroupObjects);
    }

    @IdentityStartLazy
    public ImSet<ObjectEntity> getObjectInstances() { 
        MAddSet<ActionOrPropertyObjectEntity<?, ?>> propertyObjects = SetFact.mAddSet();

        // todo [dale]: READONLYIF is absent in the list, but may be it should be in some cases
        PropertyDrawExtraType[] neededTypes = {CAPTION, FOOTER, SHOWIF, BACKGROUND, FOREGROUND};
        for (PropertyDrawExtraType type : neededTypes) {
            PropertyObjectEntity<?> prop = propertyExtras.get(type);
            if (prop != null) {
                propertyObjects.add(prop);
            }
        }
        
        MSet<ObjectEntity> mObjects = SetFact.mSet();
        for(int i=0,size=propertyObjects.size();i<size;i++)
            mObjects.addAll(propertyObjects.get(i).getObjectInstances());
        mObjects.addAll(getValueActionOrProperty().getObjectInstances());
        if(toDraw != null)
            mObjects.add(toDraw.getOrderObjects().get(0));
        return mObjects.immutable();
    }

    public GroupObjectEntity getNFToDraw(FormEntity form, Version version) {
        return toDraw==null?form.getNFApplyObject(getObjectInstances(), version):toDraw;
    }

    public boolean isToolbar(FormEntity formEntity) {
        if (forceViewType != null)
            return forceViewType.isToolbar();

        GroupObjectEntity toDraw = getToDraw(formEntity);
        return toDraw != null && toDraw.classView.isToolbar();
    }

    public boolean isGrid(FormEntity formEntity) {
        GroupObjectEntity toDraw = getToDraw(formEntity);
        return toDraw != null && toDraw.classView.isGrid() && (forceViewType == null || forceViewType.isGrid());
    }

    static public String createSID(String name, List<String> mapping) {
        StringBuilder sidBuilder = new StringBuilder();
        sidBuilder.append(name);
        sidBuilder.append("(");
        for (int i = 0; i < mapping.size(); i++) {
            if (i > 0) {
                sidBuilder.append(",");
            }
            sidBuilder.append(mapping.get(i));
        }
        sidBuilder.append(")");
        return sidBuilder.toString();        
    }

    public static <P extends PropertyInterface> String createSID(ActionOrPropertyObjectEntity<?, ?> property, ImOrderSet<P> interfaces) {
        assert property.property.isNamed();
        List<String> mapping = new ArrayList<>();
        for (P pi : interfaces)
            mapping.add(property.mapping.getObject(pi).getSID());
        return createSID(property.property.getName(), mapping);
    }

    public String getFormPath() {
        return formPath;
    }

    public void setFormPath(String formPath) {
        this.formPath = formPath;
    }

    public Pair<Integer, Integer> getScriptIndex() {
        return scriptIndex;
    }

    public void setScriptIndex(Pair<Integer, Integer> scriptIndex) {
        this.scriptIndex = scriptIndex;
    }

    @Override
    public Object getProfiledObject() {
        return this;
    }

    public byte getTypeID() {
        return PropertyReadType.DRAW;
    }

    public Type getType() {
        return getValueProperty().property.getType();
    }

    public LocalizedString getCaption() {
        return getInheritedProperty().caption;
    }

    public boolean isNotNull() {
        return getInheritedProperty().isNotNull();
    }

    public void deny(ViewPropertySecurityPolicy policy) {
        policy.deny(getSecurityProperty());
    }

    public String integrationSID; // hack - can be null for EXPORT FROM orders

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getIntegrationSID() {
        return integrationSID;
    }
    
    public PropertyObjectEntity getImportProperty() {
        return (PropertyObjectEntity) propertyObject;
    }

    // for getExpr, getType purposes
    public ActionOrPropertyObjectEntity<?, ?> getValueActionOrProperty() {
        return propertyObject;
    }

    public PropertyObjectEntity<?> getValueProperty() {
        return (PropertyObjectEntity) getValueActionOrProperty();
    }

    @Override
    public PropertyObjectEntity getPropertyObjectEntity() {
        return getValueProperty();
    }

    // presentation info, probably should be merged with inheritDrawOptions mechanism
    public ActionOrProperty getInheritedProperty() {
        return inheritedProperty;
    }

    public ActionOrProperty getSecurityProperty() {
        return getInheritedProperty();
    }

    // for debug purposes
    public ActionOrProperty getDebugBindingProperty() {
        return getInheritedProperty();
    }
    public ActionOrPropertyObjectEntity getDebugProperty() {
        return propertyObject;
    }

    @Override
    public String getReportSID() {
        return getSID();
    }
}
