package lsfusion.client.navigator.controller.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.client.controller.remote.proxy.PendingRemoteObjectProxy;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.navigator.ClientSettings;
import lsfusion.interop.navigator.remote.ClientCallBackInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface> extends PendingRemoteObjectProxy<T> implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    public void logClientException(String hostname, Throwable t) throws RemoteException {
        target.logClientException(hostname, t);
    }

    public void close() throws RemoteException {
        logRemoteMethodStartCall("close");
        target.close();
        logRemoteMethodEndCall("close", "");
    }

    public ClientCallBackInterface getClientCallBack() throws RemoteException {
        return target.getClientCallBack();
    }

    @Override
    public Pair<RemoteFormInterface, String> createFormExternal(String json) throws RemoteException {
        return target.createFormExternal(json);
    }

    public byte[] getNavigatorTree() throws RemoteException {
        try {
            return callImmutableMethod("getNavigatorTree", new Callable<byte[]>() {
                @Override
                public byte[] call() throws Exception {
                    return target.getNavigatorTree();
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public ClientSettings getClientSettings() throws RemoteException {
        return target.getClientSettings();
    }

    @Override
    public ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, String script) throws RemoteException {
        return target.executeNavigatorAction(requestIndex, lastReceivedRequestIndex, script);
    }

    @Override
    public ServerResponse executeNavigatorAction(long requestIndex, long lastReceivedRequestIndex, String navigatorActionSID, int type) throws RemoteException {
        return target.executeNavigatorAction(requestIndex, lastReceivedRequestIndex, navigatorActionSID, type);
    }

    @Override
    public ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object[] actionResults) throws RemoteException {
        return target.continueServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults);
    }

    @Override
    public ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable clientThrowable) throws RemoteException {
        return target.throwInServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, clientThrowable);
    }

    @Override
    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        return target.isInServerInvocation(requestIndex);
    }
}
