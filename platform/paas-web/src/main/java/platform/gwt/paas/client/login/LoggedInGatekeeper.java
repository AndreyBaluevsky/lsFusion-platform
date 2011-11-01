package platform.gwt.paas.client.login;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.dispatch.shared.DispatchAsync;
import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import platform.gwt.paas.client.PaasPlaceManager;
import platform.gwt.paas.client.common.ErrorHandlingCallback;
import platform.gwt.paas.shared.actions.LogoutAction;
import platform.gwt.paas.shared.actions.VoidResult;

public class LoggedInGatekeeper implements Gatekeeper {

    @Inject
    private PaasPlaceManager placeManager;

    @Inject
    DispatchAsync dispatcher;

    private CurrentUser currentUser = null;

    @Inject
    public LoggedInGatekeeper(final EventBus eventBus) {
        eventBus.addHandler(LoginAuthenticatedEvent.TYPE, new LoginAuthenticatedEventHandler() {
            @Override
            public void onLogin(LoginAuthenticatedEvent event) {
                currentUser = event.getCurrentUser();
                Log.debug(currentUser.getLogin() + " credentials have been authenticated.");
            }
        });

        eventBus.addHandler(LogoutAuthenticatedEvent.TYPE, new LogoutAuthenticatedEventHandler() {
            @Override
            public void onLogout(LogoutAuthenticatedEvent event) {
                dispatcher.execute(new LogoutAction(), new ErrorHandlingCallback<VoidResult>() {
                    @Override
                    public void success(VoidResult result) {
                        currentUser = null;
                        Log.debug("User logouted");
                        placeManager.revealDefaultPlace();
                    }
                });
            }
        });
    }

    @Override
    public boolean canReveal() {
        boolean loggedIn = false;

        if (currentUser != null) {
            loggedIn = currentUser.isLoggedIn();
        }

        return loggedIn;
    }
}
