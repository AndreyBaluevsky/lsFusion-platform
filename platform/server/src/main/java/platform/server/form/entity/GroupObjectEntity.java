package platform.server.form.entity;

import platform.base.BaseUtils;
import platform.base.identity.IdentityObject;
import platform.interop.ClassViewType;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupObjectEntity extends IdentityObject implements Instantiable<GroupObjectInstance>, ServerIdentitySerializable {

    public static int PAGE_SIZE_DEFAULT_VALUE = 10;

    private int ID;

    public PropertyDrawEntity filterProperty;

    public TreeGroupEntity treeGroup;

    public List<ObjectEntity> objects = new ArrayList<ObjectEntity>();

    public GroupObjectEntity() {
    }

    public GroupObjectEntity(int ID) {
        this.ID = ID;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public boolean add(ObjectEntity objectEntity) {
        objectEntity.groupTo = this;
        return objects.add(objectEntity);
    }

    public ClassViewType initClassView = ClassViewType.GRID;
    public List<ClassViewType> banClassView = new ArrayList<ClassViewType>();
    public Integer pageSize;
    public Boolean needHorizontalScroll;
    public Integer tableRowsCount;

    public PropertyObjectEntity<?> propertyHighlight;

    public GroupObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, objects);
        pool.writeInt(outStream, initClassView.ordinal());
        pool.writeObject(outStream, banClassView);
        pool.serializeObject(outStream, treeGroup);
        pool.serializeObject(outStream, propertyHighlight);
        pool.serializeObject(outStream, filterProperty);
        outStream.writeBoolean(isParent != null);
        if (isParent != null) {
            pool.serializeMap(outStream, isParent);
        }
        pool.writeObject(outStream, pageSize);
        pool.writeObject(outStream, needHorizontalScroll);
        pool.writeInt(outStream, tableRowsCount);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        pool.deserializeCollection(objects, inStream);
        initClassView = ClassViewType.values()[pool.readInt(inStream)];
        banClassView = pool.readObject(inStream);
        treeGroup = pool.deserializeObject(inStream);
        propertyHighlight = pool.deserializeObject(inStream);
        filterProperty = pool.deserializeObject(inStream);
        if (inStream.readBoolean()) {
            isParent = pool.deserializeMap(inStream);
        }
        pageSize = pool.readObject(inStream);
        needHorizontalScroll = pool.readObject(inStream);
        tableRowsCount = pool.readInt(inStream);
    }

    public Map<ObjectEntity, PropertyObjectEntity> isParent = null;

    public void setIsParents(PropertyObjectEntity... properties) {
        isParent = new HashMap<ObjectEntity, PropertyObjectEntity>();
        for (int i = 0; i < objects.size(); i++) {
            isParent.put(objects.get(i), properties[i]);
        }
    }

    public void setSingleClassView(ClassViewType type) {
        initClassView = type;
        banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.GRID, ClassViewType.HIDE));
        banClassView.remove(type);
    }

    public boolean isAllowedClassView(ClassViewType type) {
        return !banClassView.contains(type);
    }
}
