package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.AbstractForm;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.SIDHandler;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderMap;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerCustomSerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectView;
import lsfusion.server.logics.form.interactive.design.object.ObjectView;
import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.synchronizedMap;
import static lsfusion.server.logics.form.interactive.design.object.GroupObjectContainerSet.*;

public class FormView extends IdentityObject implements ServerCustomSerializable, AbstractForm<ContainerView, ComponentView, LocalizedString> {

    // нужен для того, чтобы генерировать уникальный идентификаторы объектам рисования, для передачи их клиенту
    public final IDGenerator idGenerator = new DefaultIDGenerator();

    public FormEntity entity;

    public KeyStroke keyStroke = null;

    public LocalizedString caption = LocalizedString.NONAME;
    public String canonicalName = "";
    public String creationPath = "";

    public Integer overridePageWidth;

    public int autoRefresh = 0;

    // список деревеьев
    private NFSet<TreeGroupView> treeGroups = NFFact.set();
    public ImSet<TreeGroupView> getTreeGroups() {
        return treeGroups.getSet();
    }
    public Iterable<TreeGroupView> getTreeGroupsIt() {
        return treeGroups.getIt();
    }

    // список групп
    public NFOrderSet<GroupObjectView> groupObjects = NFFact.orderSet();
    public Iterable<GroupObjectView> getGroupObjectsIt() {
        return groupObjects.getIt();
    }
    public ImOrderSet<GroupObjectView> getGroupObjectsListIt() {
        return groupObjects.getOrderSet();
    }
    public Iterable<GroupObjectView> getNFGroupObjectsIt(Version version) {
        return groupObjects.getNFIt(version); 
    }
    public Iterable<GroupObjectView> getNFGroupObjectsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return groupObjects.getNFListIt(version);
    }

    // список свойств
    public NFOrderSet<PropertyDrawView> properties = NFFact.orderSet();
    public Iterable<PropertyDrawView> getPropertiesIt() {
        return properties.getIt();
    }
    public ImOrderSet<PropertyDrawView> getPropertiesList() {
        return properties.getOrderSet();
    }
    public Iterable<PropertyDrawView> getNFPropertiesIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return properties.getNFIt(version);
    }
    public Iterable<PropertyDrawView> getNFPropertiesListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return properties.getNFListIt(version);
    }

    // список фильтров
    public NFOrderSet<RegularFilterGroupView> regularFilters = NFFact.orderSet();
    public Iterable<RegularFilterGroupView> getRegularFiltersIt() {
        return regularFilters.getIt();
    }
    public ImOrderSet<RegularFilterGroupView> getRegularFiltersList() {
        return regularFilters.getOrderSet();
    }
    public Iterable<RegularFilterGroupView> getNFRegularFiltersListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return regularFilters.getNFListIt(version);
    }

    protected NFOrderMap<PropertyDrawView,Boolean> defaultOrders = NFFact.orderMap();
    public ImOrderMap<PropertyDrawView, Boolean> getDefaultOrders() {
        return defaultOrders.getListMap();
    }

    public ContainerView mainContainer;

    protected PropertyDrawView printButton;
    protected PropertyDrawView editButton;
    protected PropertyDrawView xlsButton;
    protected PropertyDrawView dropButton;
    protected PropertyDrawView refreshButton;
    protected PropertyDrawView applyButton;
    protected PropertyDrawView cancelButton;
    protected PropertyDrawView okButton;
    protected PropertyDrawView closeButton;

    protected transient Map<TreeGroupEntity, TreeGroupView> mtreeGroups = synchronizedMap(new HashMap<>());
    public TreeGroupView get(TreeGroupEntity treeGroup) { return mtreeGroups.get(treeGroup); }

    protected transient Map<GroupObjectEntity, GroupObjectView> mgroupObjects = synchronizedMap(new HashMap<>());
    public GroupObjectView get(GroupObjectEntity groupObject) { return mgroupObjects.get(groupObject); }

    protected transient Map<ObjectEntity, ObjectView> mobjects = synchronizedMap(new HashMap<>());
    public ObjectView get(ObjectEntity object) { return mobjects.get(object); }

    protected transient Map<PropertyDrawEntity, PropertyDrawView> mproperties = synchronizedMap(new HashMap<>());
    public PropertyDrawView get(PropertyDrawEntity property) { return mproperties.get(property); }

    protected transient Map<RegularFilterGroupEntity, RegularFilterGroupView> mfilters = synchronizedMap(new HashMap<>());
    public RegularFilterGroupView get(RegularFilterGroupEntity filterGroup) { return mfilters.get(filterGroup); }

    public ComponentView findById(int id) {
        return mainContainer.findById(id);
    }

    public FormView() {
    }

    public FormView(FormEntity entity, Version version) {
        super(0);

        idGenerator.idRegister(0);
        
        this.entity = entity;

        mainContainer = new ContainerView(idGenerator.idShift(), true);
        setComponentSID(mainContainer, getBoxContainerSID(), version);

        for (GroupObjectEntity group : entity.getNFGroupsListIt(version)) {
            addGroupObjectBase(group, version);
        }

        for (TreeGroupEntity treeGroup : entity.getNFTreeGroupsIt(version)) {
            addTreeGroupBase(treeGroup, version);
        }

        for (PropertyDrawEntity property : entity.getNFPropertyDrawsListIt(version)) {
            PropertyDrawView view = addPropertyDrawBase(property, version);
            view.caption = property.initCaption;
        }

        for (RegularFilterGroupEntity filterGroup : entity.getNFRegularFilterGroupsListIt(version)) {
            addRegularFilterGroupBase(filterGroup, version);
        }

        initButtons(version);
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean ascending, Version version) {
        defaultOrders.add(get(property), ascending, version);
    }

    private void addPropertyDrawView(PropertyDrawView property) {
        mproperties.put(property.entity, property);
    }

    private PropertyDrawView addPropertyDrawBase(PropertyDrawEntity property, Version version) {
        PropertyDrawView propertyView = new PropertyDrawView(property);
        properties.add(propertyView, version);
        addPropertyDrawView(propertyView);

        //походу инициализируем порядки по умолчанию
        Boolean ascending = entity.getNFDefaultOrder(property, version);
        if (ascending != null) {
            defaultOrders.add(propertyView, ascending, version);
        }

        return propertyView;
    }

    public PropertyDrawView addPropertyDraw(PropertyDrawEntity property, Version version) {
        return addPropertyDrawBase(property, version);
    }

    public void movePropertyDrawTo(PropertyDrawEntity property, PropertyDrawEntity newNeighbour, boolean isRightNeighbour, Version version) {
        PropertyDrawView propertyView = mproperties.get(property);
        PropertyDrawView neighbourView = mproperties.get(newNeighbour);
        assert propertyView != null && neighbourView != null;

        properties.move(propertyView, neighbourView, isRightNeighbour, version);
    }

    private void addGroupObjectView(GroupObjectView groupObjectView, Version version) {
        mgroupObjects.put(groupObjectView.entity, groupObjectView);

        boolean isInTree = groupObjectView.entity.isInTree();

        if(!isInTree) { // правильнее вообще не создавать компоненты, но для этого потребуется более сложный рефакторинг, поэтому пока просто сделаем так чтобы к ним нельзя было обратиться
            setComponentSID(groupObjectView.getGrid(), getGridSID(groupObjectView), version);
            setComponentSID(groupObjectView.getToolbarSystem(), getToolbarSystemSID(groupObjectView), version);
            setComponentSID(groupObjectView.getUserFilter(), getUserFilterSID(groupObjectView), version);
            setComponentSID(groupObjectView.getCalculations(), getCalculationsSID(groupObjectView), version);
        }

        for (ObjectView object : groupObjectView) {
            mobjects.put(object.entity, object);
            if(!isInTree)
                setComponentSID(object.classChooser, getClassChooserSID(object.entity), version);
        }
    }
    
    public GroupObjectView addGroupObjectBase(GroupObjectEntity groupObject, GroupObjectEntity neighbour, Boolean isRightNeighbour, Version version) {
        GroupObjectView groupObjectView = new GroupObjectView(idGenerator, groupObject);
        if (neighbour != null) {
            groupObjects.addIfNotExistsToThenLast(groupObjectView, get(neighbour), isRightNeighbour != null && isRightNeighbour, version);
        } else {
            groupObjects.add(groupObjectView, version);
        }
        addGroupObjectView(groupObjectView, version);
        return groupObjectView;    
    }

    private GroupObjectView addGroupObjectBase(GroupObjectEntity groupObject, Version version) {
        return addGroupObjectBase(groupObject, null, null, version);
    }

    private TreeGroupView addTreeGroupBase(TreeGroupEntity treeGroup, Version version) {
        return addTreeGroupBase(treeGroup, null, false, version);
    }

    public GroupObjectView addGroupObject(GroupObjectEntity groupObject, GroupObjectEntity neighbour, Boolean isRightNeighbour, Version version) {
        return addGroupObjectBase(groupObject, neighbour, isRightNeighbour, version);
    }

    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup, GroupObjectEntity neighbour, boolean isRightNeighbour, Version version) {
        return addTreeGroupBase(treeGroup, neighbour, isRightNeighbour, version);
    }

    private void addTreeGroupView(TreeGroupView treeGroupView, Version version) {
        mtreeGroups.put(treeGroupView.entity, treeGroupView);
        setComponentSID(treeGroupView, getGridSID(treeGroupView), version);
        setComponentSID(treeGroupView.getToolbarSystem(), getToolbarSystemSID(treeGroupView), version);
        setComponentSID(treeGroupView.getUserFilter(), getUserFilterSID(treeGroupView), version);
    }

    private TreeGroupView addTreeGroupBase(TreeGroupEntity treeGroup, GroupObjectEntity neighbourGroupObject, boolean isRightNeighbour, Version version) {
        TreeGroupView treeGroupView = new TreeGroupView(this, treeGroup, version);
        treeGroups.add(treeGroupView, version);
        addTreeGroupView(treeGroupView, version);
        return treeGroupView;
    }

    private void addRegularFilterGroupView(RegularFilterGroupView filterGroupView, Version version) {
        mfilters.put(filterGroupView.entity, filterGroupView);
        setComponentSID(filterGroupView, getFilterGroupSID(filterGroupView.entity), version);
    }

    private RegularFilterGroupView addRegularFilterGroupBase(RegularFilterGroupEntity filterGroup, Version version) {
        RegularFilterGroupView filterGroupView = new RegularFilterGroupView(filterGroup, version);
        regularFilters.add(filterGroupView, version);
        addRegularFilterGroupView(filterGroupView, version);
        return filterGroupView;
    }

    public RegularFilterGroupView addRegularFilterGroup(RegularFilterGroupEntity filterGroupEntity, Version version) {
        return addRegularFilterGroupBase(filterGroupEntity, version);
    }
    
    public RegularFilterView addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, Version version) {
        RegularFilterGroupView filterGroupView = get(filterGroup);
        return filterGroupView.addFilter(filter, version);
    }

    public void fillComponentMaps() {
        for (GroupObjectView group : getGroupObjectsIt()) {
            addGroupObjectView(group, Version.descriptor());
        }

        for (TreeGroupView treeGroup : getTreeGroupsIt()) {
            addTreeGroupView(treeGroup, Version.descriptor());
        }

        for (PropertyDrawView property : getPropertiesIt()) {
            addPropertyDrawView(property);
        }

        for (RegularFilterGroupView filterGroup : getRegularFiltersIt()) {
            addRegularFilterGroupView(filterGroup, Version.descriptor());
        }

        initButtons(Version.descriptor());
    }

    private void initButtons(Version version) {
        editButton = getNFProperty(entity.editActionPropertyDraw, version);
        refreshButton = getNFProperty(entity.refreshActionPropertyDraw, version);
        applyButton = getNFProperty(entity.applyActionPropertyDraw, version);
        cancelButton = getNFProperty(entity.cancelActionPropertyDraw, version);
        okButton = getNFProperty(entity.okActionPropertyDraw, version);
        closeButton = getNFProperty(entity.closeActionPropertyDraw, version);
        dropButton = getNFProperty(entity.dropActionPropertyDraw, version);
    }

    public ContainerView createContainer(Version version) {
        return createContainer(null, version);
    }

    public ContainerView createContainer(LocalizedString caption, Version version) {
        return createContainer(caption, null, null, version);
    }

    public ContainerView createContainer(LocalizedString caption, LocalizedString description, String sID, Version version) {
        ContainerView container = new ContainerView(idGenerator.idShift());
        
        // Не используем здесь setCaption и setDescription из-за того, что они принимают на вход String.
        // Изменить тип, принимаемый set методами не можем, потому что этот интерфейс используется и на клиенте, где
        // LocalizedString отсутствует.
        container.caption = caption;
        container.description = description;
        
        container.setSID(sID);
        if (sID != null) {
            addComponentToMapping(container, version);
        }
        return container;
    }

    private final SIDHandler<ComponentView> componentSIDHandler = new SIDHandler<ComponentView>() {
        public boolean checkUnique() {
            return false;
        }

        protected String getSID(ComponentView component) {
            return component.getSID();
        }
    };
    
    public void addComponentToMapping(ComponentView container, Version version) {
        componentSIDHandler.store(container, version);
    }

    public void removeContainerFromMapping(ContainerView container, Version version) {
        componentSIDHandler.remove(container, version);
    }

    public ComponentView getComponentBySID(String sid, Version version) {
        return componentSIDHandler.find(sid, version);
    }

    public PropertyDrawView getPrintButton() {
        return printButton;
    }

    public PropertyDrawView getEditButton() {
        return editButton;
    }

    public PropertyDrawView getXlsButton() {
        return xlsButton;
    }

    public PropertyDrawView getDropButton() {
        return dropButton;
    }

    public PropertyDrawView getRefreshButton() {
        return refreshButton;
    }

    public PropertyDrawView getApplyButton() {
        return applyButton;
    }

    public PropertyDrawView getCancelButton() {
        return cancelButton;
    }

    public PropertyDrawView getOkButton() {
        return okButton;
    }

    public PropertyDrawView getCloseButton() {
        return closeButton;
    }

    public ContainerView getMainContainer() {
        return mainContainer;
    }

    public GroupObjectView getGroupObject(GroupObjectEntity entity) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : getGroupObjectsIt())
            if (entity.equals(groupObject.entity))
                return groupObject;
        return null;
    }

    public GroupObjectView getNFGroupObject(GroupObjectEntity entity, Version version) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : getNFGroupObjectsIt(version))
            if (entity.equals(groupObject.entity))
                return groupObject;
        return null;
    }

    public ObjectView getObject(ObjectEntity entity) {
        if (entity == null) {
            return null;
        }
        for (GroupObjectView groupObject : getGroupObjectsIt())
            for(ObjectView object : groupObject)
                if (entity.equals(object.entity))
                    return object;
        return null;
    }

    public TreeGroupView getTreeGroup(TreeGroupEntity entity) {
        if (entity == null) {
            return null;
        }
        for (TreeGroupView treeGroup : getTreeGroupsIt())
            if (entity.equals(treeGroup.entity))
                return treeGroup;
        return null;
    }

    public PropertyDrawView getProperty(PropertyDrawEntity entity) {
        if (entity == null) {
            return null;
        }
        for (PropertyDrawView property : getPropertiesIt()) {
            if (entity.equals(property.entity)) {
                return property;
            }
        }
        return null;
    }

    public PropertyDrawView getNFProperty(PropertyDrawEntity entity, Version version) {
        if (entity == null) {
            return null;
        }
        for (PropertyDrawView property : getNFPropertiesIt(version)) {
            if (entity.equals(property.entity)) {
                return property;
            }
        }
        return null;
    }

    public List<PropertyDrawView> getProperties(GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<>();

        for (PropertyDrawView property : getPropertiesIt()) {
            if (groupObject.equals(property.entity.getToDraw(entity))) {
                result.add(property);
            }
        }

        return result;
    }

    public void setFont(FontInfo font) {

        for (PropertyDrawView property : getPropertiesIt()) {
            setFont(property, font);
        }
    }

    public void setFont(FontInfo font, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(PropertyDrawView property, FontInfo font) {
        property.design.setFont(font);
    }

    public void setCaptionFont(FontInfo captionFont) {
        for (PropertyDrawView property : getPropertiesIt()) {
            setCaptionFont(property, captionFont);
        }
    }

    public void setCaptionFont(FontInfo captionFont, GroupObjectEntity groupObject) {
        for (PropertyDrawView property : getProperties(groupObject)) {
            setCaptionFont(property, captionFont);
        }
    }

    public void setCaptionFont(PropertyDrawView property, FontInfo captionFont) {
        property.design.setCaptionFont(captionFont);
    }

    public void setBackground(PropertyDrawView property, Color background) {
        property.design.background = background;
    }

    public void setFocusable(boolean focusable, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(groupObject)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(ObjectEntity objectEntity, boolean focusable) {
        for (PropertyDrawView property : getProperties(objectEntity.groupTo)) {
            setFocusable(property, focusable);
        }
    }

    public void setFocusable(PropertyDrawView property, boolean focusable) {
        property.focusable = focusable;
    }

    public void setEditOnSingleClick(PropertyDrawView property, Boolean editOnSingleClick) {
        property.editOnSingleClick = editOnSingleClick;
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        this.keyStroke = keyStroke;
    }

    public void setCaption(LocalizedString caption) {
        this.caption = caption;
    }

    public void setChangeKey(PropertyDrawView property, KeyStroke keyStroke) {
        property.changeKey = keyStroke != null ? new KeyInputEvent(keyStroke) : null;
    }
    public void setChangeMouse(PropertyDrawView property, String mouseStroke) {
        property.changeMouse = new MouseInputEvent(mouseStroke);
    }

    public void setPanelCaptionAbove(PropertyDrawView property, boolean panelCaptionAbove) {
        property.panelCaptionAbove = panelCaptionAbove;
    }

    public void setPropertyDrawViewHide(boolean hide, PropertyDrawEntity... properties) {
        for (PropertyDrawEntity property : properties) {
            setPropertyDrawViewHide(property, hide);
        }
    }

    public void setPropertyDrawViewHide(PropertyDrawEntity property, boolean hide) {
        getProperty(property).hide = hide;
    }

    protected void setComponentSID(ComponentView component, String sid, Version version) {
        component.setSID(sid);
        addComponentToMapping(component, version);
    }

    public ContainerView getContainerBySID(String sid, Version version) {
        ComponentView component = getComponentBySID(sid, version);
        if (component != null && !(component instanceof ContainerView)) {
            throw new IllegalStateException(sid + " component has to be container");
        }
        return (ContainerView) component;
    }

    private static String getBoxContainerSID() {
        return FormContainerSet.BOX_CONTAINER;
    }

    private static String getFilterGroupSID(RegularFilterGroupEntity entity) {
        return FILTERGROUP_COMPONENT + "(" + entity.getSID() + ")";
    }

    private static String getGridSID(PropertyGroupContainerView entity) {
        return GRID_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }

    private static String getToolbarSystemSID(PropertyGroupContainerView entity) {
        return TOOLBAR_SYSTEM_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }

    private static String getUserFilterSID(PropertyGroupContainerView entity) {
        return USERFILTER_COMPONENT + "(" + entity.getPropertyGroupContainerSID() + ")";
    }
    
    private static String getCalculationsSID(PropertyGroupContainerView entity) {
        return entity.getPropertyGroupContainerSID() + ".calculations";
    }

    private static String getClassChooserSID(ObjectEntity entity) {
        return CLASSCHOOSER_COMPONENT + "(" + entity.getSID() + ")";
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.serializeObject(outStream, mainContainer);
        pool.serializeCollection(outStream, getTreeGroups());
        pool.serializeCollection(outStream, getGroupObjectsListIt());
        pool.serializeCollection(outStream, getPropertiesList());
        pool.serializeCollection(outStream, getRegularFiltersList());

        ImOrderMap<PropertyDrawView, Boolean> defaultOrders = getDefaultOrders();
        int size = defaultOrders.size();
        outStream.writeInt(size);
        for (int i=0;i<size;i++) {
            pool.serializeObject(outStream, defaultOrders.getKey(i));
            outStream.writeBoolean(defaultOrders.getValue(i));
        }

        pool.writeObject(outStream, keyStroke);
        pool.writeString(outStream, ThreadLocalContext.localize(caption));
        pool.writeString(outStream, canonicalName);
        pool.writeString(outStream, creationPath);
        pool.writeInt(outStream, overridePageWidth);
        outStream.writeInt(autoRefresh);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        mainContainer = pool.deserializeObject(inStream);
        treeGroups = NFFact.finalSet(pool.deserializeSet(inStream));
        groupObjects = NFFact.finalOrderSet(pool.deserializeList(inStream));
        properties = NFFact.finalOrderSet(pool.deserializeList(inStream));
        regularFilters = NFFact.finalOrderSet(pool.deserializeList(inStream));

        int orderCount = inStream.readInt();
        MOrderExclMap<PropertyDrawView, Boolean> mDefaultOrders = MapFact.mOrderExclMap(orderCount);
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawView order = pool.deserializeObject(inStream);
            mDefaultOrders.exclAdd(order, inStream.readBoolean());
        }
        defaultOrders = NFFact.finalOrderMap(mDefaultOrders.immutableOrder());

        keyStroke = pool.readObject(inStream);
        caption = LocalizedString.create(pool.readString(inStream));
        canonicalName = pool.readString(inStream);
        creationPath = pool.readString(inStream);
        overridePageWidth = pool.readInt(inStream);
        autoRefresh = inStream.readInt();

        entity = pool.context.entity;

        fillComponentMaps();
    }

    public void finalizeAroundInit() {
        treeGroups.finalizeChanges();
        groupObjects.finalizeChanges();
        
        for(TreeGroupView property : getTreeGroupsIt())
            property.finalizeAroundInit();

        for(GroupObjectView property : getGroupObjectsIt())
            property.finalizeAroundInit();

        for(PropertyDrawView property : getPropertiesIt())
            property.finalizeAroundInit();

        defaultOrders.finalizeChanges();

        for(RegularFilterGroupView regularFilter : getRegularFiltersIt())
            regularFilter.finalizeAroundInit();
                
        mainContainer.finalizeAroundInit();
        componentSIDHandler.finalizeChanges();
    }
}