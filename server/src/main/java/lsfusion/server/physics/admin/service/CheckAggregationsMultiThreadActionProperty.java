package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.admin.service.task.CheckAggregationsTask;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class CheckAggregationsMultiThreadActionProperty extends MultiThreadActionProperty {

    public CheckAggregationsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new CheckAggregationsTask();
    }

    @Override
    protected String getCaptionError() {
        return localize("{logics.check.aggregation.error}");
    }

    @Override
    protected MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new MessageClientAction(localize(LocalizedString.createFormatted(errorOccurred ? "{logics.check.failed}" : "{logics.check.completed}",
                localize("{logics.checking.aggregations}"))) + task.getMessages(), localize("{logics.checking.aggregations}"), true);
    }
}