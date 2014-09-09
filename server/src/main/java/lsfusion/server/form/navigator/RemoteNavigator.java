package lsfusion.server.form.navigator;

// навигатор работает с абстрактной BL

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.server.ServerLoggers;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.auth.User;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.context.ContextAwareDaemonThreadFactory;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.instance.GroupObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.instance.listener.RemoteFormListener;
import lsfusion.server.logics.*;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.remote.ContextAwarePendingRemoteObject;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.remote.RemotePausableInvocation;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static lsfusion.base.BaseUtils.nvl;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends ContextAwarePendingRemoteObject implements RemoteNavigatorInterface, FocusListener<T>, CustomClassListener, RemoteFormListener, Unreferenced {
    protected final static Logger logger = ServerLoggers.systemLogger;

    SQLSession sql;

    final LogicsInstance logicsInstance;
    private final NavigatorsManager navigatorManager;
    private final BusinessLogics businessLogics;
    private final SecurityManager securityManager;
    private final DBManager dbManager;

    // просто закэшируем, чтобы быстрее было
    SecurityPolicy securityPolicy;

    private DataObject user;

    private DataObject computer;

    private DataObject connection;

    private int updateTime;

    private String remoteAddress;

    private final WeakIdentityHashSet<DataSession> sessions = new WeakIdentityHashSet<DataSession>();

    private final boolean isFullClient;

    private ClientCallBackController client;

    private final ExecutorService pausablesExecutor;
    private RemotePausableInvocation currentInvocation = null;
    
    private final Map<RemoteForm, Boolean> createdForms = Collections.synchronizedMap(new WeakHashMap<RemoteForm, Boolean>());

    private static final List<Pair<DataObject, String>> recentlyOpenForms = Collections.synchronizedList(new ArrayList<Pair<DataObject, String>>());

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать
    public RemoteNavigator(LogicsInstance logicsInstance, boolean isFullClient, String remoteAddress, User currentUser, int computer, int port) throws RemoteException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(port);

        this.logicsInstance = logicsInstance;
        this.navigatorManager = logicsInstance.getNavigatorsManager();
        this.businessLogics = logicsInstance.getBusinessLogics();
        this.securityManager = logicsInstance.getSecurityManager();
        this.dbManager = logicsInstance.getDbManager();

        this.isFullClient = isFullClient;

        setContext(new RemoteNavigatorContext(this));

        pausablesExecutor = Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(context, "navigator-daemon"));

        this.client = new ClientCallBackController(port, new ClientCallBackController.UsageTracker() {
            @Override
            public void used() {
                updateLastUsedTime();
            }
        });
        this.classCache = new ClassCache();

        this.securityPolicy = currentUser.getSecurityPolicy();
        this.transactionTimeout = currentUser.getTimeout();

        this.user = new DataObject(currentUser.ID, businessLogics.authenticationLM.customUser);
        this.computer = new DataObject(computer, businessLogics.authenticationLM.computer);

        this.remoteAddress = remoteAddress;
        this.sql = dbManager.createSQL();
    }

    public boolean isFullClient() {
        return isFullClient;
    }

    public void changeCurrentUser(DataObject user) throws SQLException {
        this.user = user;
        Result<Integer> timeout = new Result<Integer>();
        this.securityPolicy = getUserSecurityPolicy(timeout);
        this.transactionTimeout = timeout.result;
        updateEnvironmentProperty((CalcProperty) businessLogics.authenticationLM.currentUser.property, user);
    }

    public void updateEnvironmentProperty(CalcProperty property, ObjectValue value) throws SQLException {
        for (DataSession session : sessions)
            session.updateProperties(SetFact.singleton(property), true); // редко используется поэтому все равно
    }

    public SecurityPolicy getUserSecurityPolicy(Result<Integer> timeout) {
        try {
            User user = securityManager.readUserWithSecurityPolicy(getUserLogin(), createSession());
            timeout.set(user.getTimeout());
            return user.getSecurityPolicy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserLogin() {
        try {
            DataSession session = createSession();
            String userLogin = (String) businessLogics.authenticationLM.loginCustomUser.read(session, user);
            session.close();
            return userLogin;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String getLogMessage() {
        return currentInvocation.getLogMessage();
    }

    @IdentityLazy
    public LogInfo getLogInfo() {
        try {
            if(closed)
                return LogInfo.system;
                
            DataSession session = createSession();
            String userName = (String) businessLogics.authenticationLM.currentUserName.read(session);
            String computerName = (String) businessLogics.authenticationLM.hostnameCurrentComputer.read(session);
            session.close();
            return new LogInfo(userName, computerName, remoteAddress);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    Object[] requestUserInteraction(final ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    public void logClientException(String title, String hostname, Throwable t) {
        String time = new SimpleDateFormat().format(Calendar.getInstance().getTime());
        logger.error(title + " at '" + time + "' from '" + hostname + "': ", t);
        try {
            businessLogics.systemEventsLM.logException(businessLogics, t, this.user, hostname, true);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public byte[] getCurrentUserInfoByteArray() {
        try {
            DataSession session = createSession();
            QueryBuilder<Object, String> query = new QueryBuilder<Object, String>(MapFact.<Object, KeyExpr>EMPTYREV());
            query.addProperty("name", businessLogics.authenticationLM.currentUserName.getExpr());
            String userName = BaseUtils.nvl((String) query.execute(session).singleValue().get("name"), "(без имени)").trim();
            session.close();

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            dataStream.writeUTF(userName);
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Aspect
    private static class RemoteNavigatorUsageAspect {
        @Around("execution(* lsfusion.interop.navigator.RemoteNavigatorInterface.*(..)) && target(remoteNavigator)")
        public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, RemoteNavigator remoteNavigator) throws Throwable {
            remoteNavigator.updateLastUsedTime();
            return thisJoinPoint.proceed();
        }
    }

    private volatile long lastUsedTime;

    public void updateLastUsedTime() {
        //забиваем на синхронизацию, потому что для времени использования совсем неактуально
        //пусть потоки меняют как хотят
        lastUsedTime = System.currentTimeMillis();
    }

    public long getLastUsedTime() {
        return lastUsedTime;
    }

    private static class WeakUserController implements UserController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakUserController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<RemoteNavigator>(navigator);
        }

        public void changeCurrentUser(DataObject user) throws SQLException {
            weakThis.get().changeCurrentUser(user);
        }

        public DataObject getCurrentUser() {
            return weakThis.get().user;
        }
    }

    private static class WeakComputerController implements ComputerController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakComputerController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<RemoteNavigator>(navigator);
        }

        public DataObject getCurrentComputer() {
            return weakThis.get().computer.getDataObject();
        }

        public boolean isFullClient() {
            return weakThis.get().isFullClient();
        }
    }

    private static class WeakTimeoutController implements TimeoutController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteNavigator> weakThis;

        private WeakTimeoutController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<RemoteNavigator>(navigator);
        }

        public int getTransactionTimeout() {
            return weakThis.get().getTransactionTimeout();
        }
    }

    private int transactionTimeout;
    public int getTransactionTimeout() {
        return transactionTimeout;
    }
    
    private DataSession createSession() throws SQLException {
        DataSession session = dbManager.createSession(sql, new WeakUserController(this), new WeakComputerController(this), new WeakTimeoutController(this), null);
        sessions.add(session);
        return session;
    }

    @Override
    public boolean isConfigurationAccessAllowed() throws RemoteException {
        return securityPolicy.configurator != null && securityPolicy.configurator;
    }

    public void gainedFocus(FormInstance<T> form) {
        //todo: не нужно, так что позже можно удалить
    }

    public static void updateOpenFormCount(BusinessLogics businessLogics) {
        try {
            DataSession session = ThreadLocalContext.getDbManager().createSession();

            try {
                List<Pair<DataObject, String>> openForms;
                synchronized (recentlyOpenForms) {
                    openForms = new ArrayList<Pair<DataObject, String>>(recentlyOpenForms);
                }
                recentlyOpenForms.clear();

                for (Pair<DataObject, String> entry : openForms) {
                    DataObject connection = entry.first;
                    String canonicalName = entry.second;
                    if (canonicalName == null) {
                        continue;
                    }

                    Integer formId = (Integer) businessLogics.reflectionLM.navigatorElementCanonicalName.read(
                            session,
                            new DataObject(canonicalName, businessLogics.reflectionLM.navigatorElementCanonicalNameClass));
                    
                    if (formId == null) {
                        continue;
                    }

                    DataObject formObject = new DataObject(formId, businessLogics.reflectionLM.navigatorElement);

                    int count = 1 + nvl((Integer) businessLogics.systemEventsLM.connectionFormCount.read(session, connection, formObject), 0);
                    businessLogics.systemEventsLM.connectionFormCount.change(count, session, connection, formObject);
                }
                session.apply(businessLogics);
            } finally {
                session.close();
            }
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Integer getObject(CustomClass cls) {
        return getCacheObject(cls);
    }

    public void objectChanged(ConcreteCustomClass cls, int objectID) {
        addCacheObject(cls, objectID);
    }

    public RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) {
        RemoteForm form = (RemoteForm) createForm(getFormEntity(formSID), isModal, interactive);
        if(initialObjects != null) {
            for (String objectSID : initialObjects.keySet()) {
                GroupObjectInstance groupObject = null;
                ObjectInstance object = null;
                for (GroupObjectInstance group : (ImOrderSet<GroupObjectInstance>) form.form.getOrderGroups()) {
                    for (ObjectInstance obj : group.objects) {
                        if (obj.getsID().equals(objectSID)) {
                            object = obj;
                            groupObject = group;
                            break;
                        }
                    }
                }
                if (object != null) {
                    groupObject.addSeek(object, new DataObject(Integer.decode(initialObjects.get(objectSID)), object.getCurrentClass()), true);
                }
            }
        }
        return form;
    }

    private FormEntity<T> getFormEntity(String formSID) {
        FormEntity<T> formEntity = (FormEntity<T>) businessLogics.getFormEntityBySID(formSID);

        if (formEntity == null) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.form.with.id.not.found") + " : " + formSID);
        }

        if (!securityPolicy.navigator.checkPermission(formEntity)) {
            return null;
        }

        return formEntity;
    }

    private RemoteFormInterface createForm(FormEntity<T> formEntity, boolean isModal, boolean interactive) {
        //todo: вернуть, когда/если починиться механизм восстановления сессии
//        try {
//            RemoteForm remoteForm = invalidatedForms.remove(formEntity);
//            if (remoteForm == null) {
//                remoteForm = context.createRemoteForm(
//                        context.createFormInstance(formEntity, MapFact.<ObjectEntity, DataObject>EMPTY(), createSession(), isModal, FormSessionScope.NEWSESSION, false, false, interactive)
//                );
//            }
//            return remoteForm;
//        } catch (Exception e) {
//            throw Throwables.propagate(e);
//        }
        try {
            return context.createRemoteForm(
                    context.createFormInstance(formEntity, MapFact.<ObjectEntity, DataObject>EMPTY(), createSession(),
                                               isModal, FormSessionScope.NEWSESSION, null, false, false, interactive, null,
                                               null, null)
            );
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    private ClassCache classCache;

    Integer getCacheObject(CustomClass cls) {
        return classCache.getObject(cls);
    }

    public void addCacheObject(ConcreteCustomClass cls, int value) {
        classCache.put(cls, value);
    }

    public DataObject getUser() {
        return user;
    }

    public DataObject getComputer() {
        return computer;
    }

    public DataObject getConnection() {
        return connection;
    }

    public void setConnection(DataObject connection) {
        this.connection = connection;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public String getRemoteAddress(){
        return remoteAddress;
    }

    public synchronized ClientCallBackController getClientCallBack() throws RemoteException {
        return client;
    }

    public boolean isRestartAllowed() {
        return client.isRestartAllowed();
    }

    public synchronized void notifyServerRestart() throws RemoteException {
        client.notifyServerRestart();
    }

    public void notifyServerRestartCanceled() throws RemoteException {
        client.notifyServerRestartCanceled();
    }

    @Override
    public DefaultFormsType showDefaultForms() throws RemoteException {
        return securityManager.showDefaultForms(user);
    }

    @Override
    public List<String> getDefaultForms() throws RemoteException {
        return securityManager.getDefaultForms(user);
    }

    @Override
    public byte[] getNavigatorTree() throws RemoteException {
        
        ImOrderMap<NavigatorElement<T>, List<String>> elements = businessLogics.LM.root.getChildrenMap(securityPolicy);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            int elementsCount = elements.size();
            
            dataStream.writeInt(elementsCount);
            for (NavigatorElement element : elements.keyIt()) {
                element.serialize(dataStream);
            }
            
            for (List<String> children : elements.valueIt()) {
                dataStream.writeInt(children.size());
                for (String child : children) {
                    dataStream.writeUTF(child);
                }
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public byte[] getCommonWindows() throws RemoteException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            businessLogics.LM.windows.log.serialize(dataStream);
            businessLogics.LM.windows.status.serialize(dataStream);
            businessLogics.LM.windows.forms.serialize(dataStream);
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public ServerResponse executeNavigatorAction(String navigatorActionSID) throws RemoteException {
        final NavigatorElement element = businessLogics.LM.root.getNavigatorElementBySID(navigatorActionSID);

        if (!(element instanceof NavigatorAction)) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.action.not.found"));
        }

        if (!securityPolicy.navigator.checkPermission(element)) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.not.enough.permissions"));
        }

        final ActionProperty property = ((NavigatorAction) element).getProperty();
        currentInvocation = new RemotePausableInvocation(pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                DataSession session = createSession();
                property.execute(MapFact.<ClassPropertyInterface, DataObject>EMPTY(), session, null);
                session.apply(businessLogics);
                session.close();
                assert !delayedGetRemoteChanges && !delayedHideForm; // тут не должно быть никаких delayRemote или hideForm
                return new ServerResponse(delayedActions.toArray(new ClientAction[delayedActions.size()]), false);
            }
        };

        return currentInvocation.execute();
    }

    @Override
    public ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException {
        return currentInvocation.resumeAfterUserInteraction(actionResults);
    }

    @Override
    public ServerResponse throwInNavigatorAction(Throwable clientThrowable) throws RemoteException {
        return currentInvocation.resumeWithThrowable(clientThrowable);
    }

    @Override
    public void formCreated(RemoteForm form) {
        DataObject connection = getConnection();
        if (connection != null) {
            recentlyOpenForms.add(new Pair<DataObject, String>(connection, form.getCanonicalName()));
        }
        createdForms.put(form, Boolean.TRUE);
    }

    @Override
    public void formDestroyed(RemoteForm form) {
        createdForms.remove(form);
    }

    @Override
    public void unexportAndClean() {
        shutdownForms();

        super.unexportAndClean();

        try {
            ThreadLocalContext.set(context);
            sql.close(OperationOwner.unknown);
        } catch (Throwable t) {
            ServerLoggers.sqlSuppLog(t);
        }
    }

    public void shutdownForms() {
        //form.unexport изменяет createdForms, поэтому работает с копией, чтобы не было ConcurrentModificationException
        Set<RemoteForm> formsCopy;
        synchronized (createdForms) {
            formsCopy = new HashSet<RemoteForm>(createdForms.keySet());
        }
        for (RemoteForm form : formsCopy) {
            if (form != null) {
                form.unexportAndClean();
            }
        }
    }

    public synchronized void close() throws RemoteException {
        ServerLoggers.assertLog(!closed, "NAVIGATOR ALREADY CLOSED");
        
        //убиваем весь remote для этого клиента сразу, чтобы не было случайных запросов
        shutdownForms();
        unexport();
        
        shutdown(false);
    }

    @Override
    public void unreferenced() {
        shutdown(true);
    }

    private boolean closed;
    private synchronized void shutdown(boolean setContext) {
        if(!closed) {
            closed = true;
            
            if(setContext)
                ThreadLocalContext.set(context);
                
            ServerLoggers.exinfoLog("NAVIGATOR CLOSE " + this + " " + sql);
            try {
                navigatorManager.navigatorClosed(this);
            } finally {
                unexportAndCleanLater();
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

    protected void finalize() throws Throwable {
        try {
            shutdown(true);
        } catch (Throwable ignored) {
        } finally {
            super.finalize();
        }
    }

    //todo: вернуть, когда/если починиться механизм восстановления сессии
//    private Map<FormEntity, RemoteForm> openForms = MapFact.mAddRemoveMap();
//    private Map<FormEntity, RemoteForm> invalidatedForms = MapFact.mAddRemoveMap();
    public synchronized void disconnect() {
        if (client != null) {
            client.disconnect();
        }

//        for (RemoteForm form : invalidatedForms.values()) {
//            form.disconnect();
//        }
    }

    @Override
    public String toString() {
        return "RemoteNavigator[clientAddress: " + remoteAddress + "]";
    }
}