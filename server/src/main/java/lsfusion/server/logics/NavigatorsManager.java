package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.server.EnvStackRunnable;
import lsfusion.interop.remote.CallbackMessage;
import lsfusion.server.ServerLoggers;
import lsfusion.server.auth.User;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.navigator.RemoteNavigator;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.LogicsManager;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class NavigatorsManager extends LogicsManager implements InitializingBean {
    private static final Logger logger = Logger.getLogger(NavigatorsManager.class);

    //время жизни неиспользуемого навигатора - 3 часа по умолчанию
    public static final long MAX_FREE_NAVIGATOR_LIFE_TIME = Long.parseLong(System.getProperty("lsfusion.server.navigatorMaxLifeTime", Long.toString(3L * 3600L * 1000L)));

    private LogicsInstance logicsInstance;

    private BusinessLogics<?> businessLogics;

    private BaseLogicsModule<?> baseLM;

    private RestartManager restartManager;

    private SecurityManager securityManager;

    private RMIManager rmiManager;

    private DBManager dbManager;

//    private ScheduledExecutorService executor;

    // synchronize'ся везде
    private final WeakIdentityHashSet<RemoteNavigator> navigators = new WeakIdentityHashSet<RemoteNavigator>();

    private AtomicBoolean removeExpiredScheduled = new AtomicBoolean(false);

    public NavigatorsManager() {
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public void setBusinessLogics(BusinessLogics<?> businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setRmiManager(RMIManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(logicsInstance, "logicsInstance must be specified");
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
        Assert.notNull(securityManager, "securityManager must be specified");
        Assert.notNull(rmiManager, "rmiManager must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        baseLM = businessLogics.LM;
//        executor = Executors.newSingleThreadScheduledExecutor(new ContextAwareDaemonThreadFactory(logicsInstance.getContext(), "navigator-manager-daemon"));
    }

    public RemoteNavigatorInterface createNavigator(ExecutionStack stack, boolean isFullClient, NavigatorInfo navigatorInfo, boolean reuseSession) {
        //пока отключаем механизм восстановления сессии... т.к. он не работает с текущей схемой последовательных запросов в форме
        reuseSession = false;

        //логика EXPIRED навигаторов неактуальна, пока не работает механизм восстановления сессии
//        scheduleRemoveExpired();

        try {
            User user;
            try (DataSession session = dbManager.createSession()) {
                user = securityManager.authenticateUser(session, navigatorInfo.login, navigatorInfo.password, stack);
            }

//            if (reuseSession) {
//                List<RemoteNavigator> navigatorsList = navigators.get(loginKey);
//                if (navigatorsList != null) {
//                    for(RemoteNavigator navigator : navigatorsList) {
//                        navigator.disconnect();
//                        navigator.unexportAndClean();
//                        removeNavigator(stack, loginKey);
//                    }
//                }
//            }

            return new RemoteNavigator(logicsInstance, isFullClient, navigatorInfo, user, rmiManager.getExportPort(), stack);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void navigatorCreated(ExecutionStack stack, RemoteNavigator navigator, NavigatorInfo navigatorInfo) throws SQLException, SQLHandledException {
        DataObject newConnection = null;

        if(!securityManager.isUniversalPassword(navigatorInfo.password)) {
            try (DataSession session = dbManager.createSession()) {
                newConnection = session.addObject(businessLogics.systemEventsLM.connection);
                businessLogics.systemEventsLM.userConnection.change(navigator.getUser().object, session, newConnection);
                businessLogics.systemEventsLM.osVersionConnection.change(navigatorInfo.osVersion, session, newConnection);
                businessLogics.systemEventsLM.processorConnection.change(navigatorInfo.processor, session, newConnection);
                businessLogics.systemEventsLM.architectureConnection.change(navigatorInfo.architecture, session, newConnection);
                businessLogics.systemEventsLM.coresConnection.change(navigatorInfo.cores, session, newConnection);
                businessLogics.systemEventsLM.physicalMemoryConnection.change(navigatorInfo.physicalMemory, session, newConnection);
                businessLogics.systemEventsLM.totalMemoryConnection.change(navigatorInfo.totalMemory, session, newConnection);
                businessLogics.systemEventsLM.maximumMemoryConnection.change(navigatorInfo.maximumMemory, session, newConnection);
                businessLogics.systemEventsLM.freeMemoryConnection.change(navigatorInfo.freeMemory, session, newConnection);
                businessLogics.systemEventsLM.javaVersionConnection.change(navigatorInfo.javaVersion, session, newConnection);
                businessLogics.systemEventsLM.screenSizeConnection.change(navigatorInfo.screenSize, session, newConnection);
                businessLogics.systemEventsLM.computerConnection.change(navigator.getComputer().object, session, newConnection);
                businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("connectedConnection"), session, newConnection);
                businessLogics.systemEventsLM.connectTimeConnection.change(businessLogics.timeLM.currentDateTime.read(session), session, newConnection);
                businessLogics.systemEventsLM.remoteAddressConnection.change(navigator.getRemoteAddress(), session, newConnection);
                session.apply(businessLogics, stack);
            }
        }

        synchronized (navigators) {
            if (newConnection != null) {
                navigator.setConnection(new DataObject(newConnection.object, businessLogics.systemEventsLM.connection));
            }
            navigators.add(navigator);
        }
    }

    public void navigatorExplicitClosed(RemoteNavigator navigator) {
        synchronized (navigators) {
            navigators.remove(navigator);
            if (navigators.isEmpty()) {
                restartManager.forcedRestartIfPending();
            }
        }
    }

    public void navigatorFinalClosed(ExecutionStack stack, RemoteNavigator navigator) {
        try {
            try (DataSession session = dbManager.createSession()) {
                if (navigator != null && navigator.getConnection() != null) {
                    businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("disconnectedConnection"), session, navigator.getConnection());
                } else
                    ServerLoggers.assertLog(false, "SHOULD NOT BE");
                session.apply(businessLogics, stack);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //    //логика EXPIRED навигаторов неактуальна, пока не работает механизм восстановления сессии
//    private synchronized void scheduleRemoveExpired() {
//        if (removeExpiredScheduled.compareAndSet(false, true)) {
//            executor.schedule(new Runnable() {
//                @Override
//                public void run() {
//                    removeNavigators(getStack(), NavigatorFilter.FALSE);
//                    removeExpiredScheduled.set(false);
//                }
//            }, 5, TimeUnit.SECONDS);
//        }
//    }

    public void updateEnvironmentProperty(CalcProperty property, ObjectValue value) throws SQLException {
        synchronized (navigators) {
            for (RemoteNavigator remoteNavigator : navigators) {
                remoteNavigator.updateEnvironmentProperty(property, value);
            }
        }
    }

    public boolean notifyServerRestart() {
        synchronized (navigators) {
            boolean canRestart = true;
            for (RemoteNavigator remoteNavigator : navigators) {
                if (!remoteNavigator.isRestartAllowed()) {
                    canRestart = false;
                    try {
                        remoteNavigator.notifyServerRestart();
                    } catch (RemoteException e) {
                        logger.error(getString("logics.server.remote.exception.on.questioning.client.for.stopping"), e);
                    }
                }
            }
            return canRestart;
        }
    }

    public void notifyServerRestartCanceled() {
        synchronized (navigators) {
            for (RemoteNavigator remoteNavigator : navigators) {
                try {
                    remoteNavigator.notifyServerRestartCanceled();
                } catch (RemoteException e) {
                    logger.error(getString("logics.server.remote.exception.on.questioning.client.for.stopping"), e);
                }
            }
        }
    }

    public void forceDisconnect(RemoteNavigator navigator, CallbackMessage message) {
        navigator.disconnect(message);
        //navigator.explicitClose(); // явное закрытие на сервере, по идее придет с клиента, но на всякий случай закроем сразу
    }

    public void forceDisconnect(ExecutionStack stack, Integer user, Integer computer, CallbackMessage message) {
        synchronized (navigators) {
            for (RemoteNavigator navigator : navigators) {
                if(navigator != null) {
                    Object navigatorComputer = navigator.getComputer().object;
                    Object navigatorUser = navigator.getUser().object;
                    if(user.equals(navigatorUser) && (computer == null || computer.equals(navigatorComputer)))
                        forceDisconnect(navigator, message);
                }
            }
        }
    }

    public void pushNotificationCustomUser(DataObject connectionObject, EnvStackRunnable run) {
        synchronized (navigators) {
            boolean found = false;
            for (RemoteNavigator navigator : navigators) {
                if(navigator != null) {
                    try {
                        if (navigator.getConnection() != null && navigator.getConnection().equals(connectionObject)) {
                            if (!found) {
                                navigator.pushNotification(run);
                                found = true;
                            } else
                                ServerLoggers.assertLog(false, "Two RemoteNavigators with same connection");
                        }
                    } catch (RemoteException e) {
                            logger.error(getString("logics.server.remote.exception.on.push.action"), e);
                    }
                }
            }
        }
    }
}
