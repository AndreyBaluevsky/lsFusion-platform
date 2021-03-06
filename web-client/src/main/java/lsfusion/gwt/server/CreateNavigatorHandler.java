package lsfusion.gwt.server;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.CreateNavigatorAction;
import lsfusion.gwt.server.logics.LogicsActionHandler;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.rmi.RemoteException;

public class CreateNavigatorHandler extends LogicsActionHandler<CreateNavigatorAction, StringResult> {

    public CreateNavigatorHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(final CreateNavigatorAction action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        return runRequest(action, new LogicsRunnable<StringResult>() {
           public StringResult run(LogicsSessionObject sessionObject) throws RemoteException {
               return new StringResult(servlet.getNavigatorProvider().createNavigator(sessionObject, servlet.getRequest()));
           }
       });
    }

    protected String getActionDetails(CreateNavigatorAction action) throws SessionInvalidatedException {
        return super.getActionDetails(action) + " TAB IN " + servlet.getSessionInfo();
    }

}
