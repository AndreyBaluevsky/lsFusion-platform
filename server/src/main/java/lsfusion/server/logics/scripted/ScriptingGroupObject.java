package lsfusion.server.logics.scripted;

import lsfusion.interop.ClassViewType;
import lsfusion.server.form.entity.ActionPropertyObjectEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.UpdateType;
import lsfusion.server.logics.i18n.LocalizedString;

import java.util.List;

public class ScriptingGroupObject {
    String groupName;
    List<String> objects;
    List<String> classes;
    List<LocalizedString> captions;
    List<ActionPropertyObjectEntity> events;
    ClassViewType viewType;
    boolean isInitType;
    Integer pageSize;
    ScriptingLogicsModule.PropertyUsage reportPathPropUsage;
    List<String> reportPathMapping;
    UpdateType updateType;
    String formObjectGroup;
    
    GroupObjectEntity neighbourGroupObject;
    Boolean isRightNeighbour;

    public ScriptingGroupObject(String name, List<String> objects, List<String> classes, List<LocalizedString> captions, List<ActionPropertyObjectEntity> events) {
        assert objects.size() == classes.size() && classes.size() == captions.size() && captions.size() == events.size();

        groupName = name;
        this.objects = objects;
        this.classes = classes;
        this.captions = captions;
        this.events = events;
    }

    public void setViewType(ClassViewType viewType) {
        this.viewType = viewType;
    }

    public void setInitType(boolean isInitType) {
        this.isInitType = isInitType;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setReportPathProp(ScriptingLogicsModule.PropertyUsage propUsage, List<String> mapping) {
        this.reportPathPropUsage = propUsage;
        this.reportPathMapping = mapping;
    }
    
    public void setUpdateType(UpdateType updateType) {
        this.updateType = updateType; 
    }
    
    public void setNeighbourGroupObject(GroupObjectEntity neighbourGroupObject, boolean isRightNeighbour) {
        this.neighbourGroupObject = neighbourGroupObject;
        this.isRightNeighbour = isRightNeighbour;
    }

    public void setFormObjectGroup(String formObjectGroup) {
        this.formObjectGroup = formObjectGroup;
    }
}
