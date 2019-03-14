package lsfusion.server.physics.admin.service;

import lsfusion.base.Result;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.sql.SQLException;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class CheckAggregationsActionProperty extends ScriptingAction {
    public CheckAggregationsActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        final Result<String> message = new Result<>();
        ServiceDBActionProperty.run(context, new RunService() {
            @Override
            public void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                message.set(context.getDbManager().checkAggregations(session));
            }
        });

        context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.check.completed}", localize("{logics.checking.aggregations}"))) + '\n' + '\n' + message.result, localize("{logics.checking.aggregations}"), true));
    }
}