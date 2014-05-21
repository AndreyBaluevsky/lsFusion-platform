package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

public class PrereadPropertyCachesTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Prereading caches for properties";
    }

    @Override
    protected boolean prerun() {
        if (SystemProperties.isDebug) {
            return false;
        }
        return true;
    }

    protected void runTask(Property property) {
        property.prereadCaches();
    }
}
