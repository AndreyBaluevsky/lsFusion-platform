package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Function;

public class TryAction extends KeepContextAction {

    private final ActionMapImplement<?, PropertyInterface> tryAction;
    private final ActionMapImplement<?, PropertyInterface> catchAction;
    private final ActionMapImplement<?, PropertyInterface> finallyAction;


    public <I extends PropertyInterface> TryAction(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                   ActionMapImplement<?, I> tryAction,
                                                   ActionMapImplement<?, I> catchAction,
                                                   ActionMapImplement<?, I> finallyAction) {
        super(caption, innerInterfaces.size());

        final ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.tryAction = tryAction.map(mapInterfaces);
        this.catchAction = catchAction == null ? null : catchAction.map(mapInterfaces);
        this.finallyAction = finallyAction == null ? null : finallyAction.map(mapInterfaces);

        finalizeInit();
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {

        MList<ActionMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        actions.add(tryAction);
        if(catchAction != null)
            actions.add(catchAction);
        if(finallyAction != null)
            actions.add(finallyAction);
        
        ImList<PropertyInterfaceImplement<PropertyInterface>> listWheres = 
                ((ImList<ActionMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        (Function<ActionMapImplement<?, PropertyInterface>, PropertyInterfaceImplement<PropertyInterface>>) ActionMapImplement::mapCalcWhereProperty);
        return PropertyFact.createUnion(interfaces, listWheres);
    }

    public ImSet<Action> getDependActions() {
        ImSet<Action> result = SetFact.EMPTY();
        result = result.merge(tryAction.action);

        if (catchAction != null) {
            result = result.merge(catchAction.action);
        }
        if (finallyAction != null) {
            result = result.merge(finallyAction.action);
        }
        return result;
    }



    @Override
    public Type getFlowSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        Type tryType = tryAction.action.getSimpleRequestInputType(optimistic, inRequest);
        Type catchType = catchAction == null ? null : catchAction.action.getSimpleRequestInputType(optimistic, inRequest);
        Type finallyType = finallyAction == null ? null : finallyAction.action.getSimpleRequestInputType(optimistic, inRequest);

        if (!optimistic) {
            if (tryType == null) {
                return null;
            }
            if (catchAction != null && catchType == null) {
                return null;
            }
            if (finallyAction != null && finallyType == null) {
                return null;
            }
        }

        Type type = tryType == null ? catchType : (catchType == null ? tryType : tryType.getCompatible(catchType));
        return type == null ? finallyType : (finallyType == null ? type : type.getCompatible(finallyType));
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        
        FlowResult result;
        FlowResult catchResult = FlowResult.FINISH;

        try {
            // since we want to catch all exceptions
            result = tryAction.execute(context.override(true));
        } catch(Throwable e) {
            if(catchAction != null) {
                context.getBL().LM.messageCaughtException.change(String.valueOf(e), context);
                context.getBL().LM.javaStackTraceCaughtException.change(ExceptionUtils.toString(e), context);
                context.getBL().LM.lsfStackTraceCaughtException.change(ExecutionStackAspect.getStackString(), context);
                catchResult = catchAction.execute(context);
            }

            //ignore exception if finallyAction == null
            if (finallyAction == null) {
                ExecutionStackAspect.getExceptionStackTrace(); // drop exception stack string
                result = catchResult;
            } else {
                throw Throwables.propagate(e);
            }
        } finally {
            if (finallyAction != null) {
                ThreadUtils.setFinallyMode(Thread.currentThread(), true);
                try {
                    finallyAction.execute(context);
                } finally {
                    ThreadUtils.setFinallyMode(Thread.currentThread(), false);
                }
            }
        }

        return result;
    }
}
