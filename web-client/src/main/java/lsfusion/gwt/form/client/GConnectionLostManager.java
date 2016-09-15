package lsfusion.gwt.form.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.WindowBox;

public class GConnectionLostManager {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();
    private static AtomicLong failedRequests = new AtomicLong();
    private static AtomicBoolean connectionLost = new AtomicBoolean(false);

    private static Timer timerWhenUnblocked;
    private static Timer timerWhenBlocked;

    private static GBlockDialog blockDialog;

    public static void start() {
        connectionLost.set(false);

        timerWhenUnblocked = new Timer() {
            @Override
            public void run() {
                GExceptionManager.flushUnreportedThrowables();
                blockIfHasFailed();
            }
        };
        timerWhenUnblocked.scheduleRepeating(1000);

        timerWhenBlocked = new Timer() {
            @Override
            public void run() {
                if (blockDialog != null) {
                    blockDialog.setFatal(isConnectionLost());
                    if (!shouldBeBlocked()) {
                        timerWhenBlocked.cancel();
                        blockDialog.hideDialog();
                        blockDialog = null;
                    }
                }
            }
        };
    }

    public static void connectionLost() {
        connectionLost.set(true);
    }

    public static boolean isConnectionLost() {
        return connectionLost.get();
    }

    public static void blockIfHasFailed() {
        if (shouldBeBlocked() && blockDialog == null) {
            blockDialog = new GBlockDialog(false, true);
            blockDialog.addOpenHandler(new OpenHandler<WindowBox>() {
                @Override
                public void onOpen(OpenEvent<WindowBox> event) {
                    if (timerWhenBlocked != null) {
                        timerWhenBlocked.scheduleRepeating(1000);
                    }
                }
            });
            blockDialog.showDialog();
        }
    }

    public static void registerFailedRmiRequest() {
        failedRequests.incrementAndGet();
    }

    public static void unregisterFailedRmiRequest() {
        failedRequests.decrementAndGet();
    }

    private static boolean hasFailedRequest() {
        return failedRequests.get() > 0;
    }

    public static boolean shouldBeBlocked() {
        return hasFailedRequest() || isConnectionLost();
    }

    public static void invalidate() {
        connectionLost();

        failedRequests.set(0);

        if (timerWhenBlocked != null) {
            timerWhenBlocked.cancel();
            timerWhenBlocked = null;
        }

        if (timerWhenUnblocked != null) {
            timerWhenUnblocked.cancel();
            timerWhenUnblocked = null;
        }

        if (blockDialog != null) {
            blockDialog.hideDialog();
            blockDialog = null;
        }
    }


    public static class GBlockDialog extends WindowBox {

        public Timer showButtonsTimer;
        private Button btnExit;
        private Button btnRelogin;
        private Button btnReconnect;
        private HTML lbMessage;
        private VerticalPanel loadingPanel;
        private VerticalPanel warningPanel;
        private VerticalPanel errorPanel;

        private boolean fatal;

        public GBlockDialog(boolean fatal, boolean showReconnect) {
            super(false, true, false, false, false);
            setGlassEnabled(true);
            this.fatal = fatal;
            lbMessage = new HTML(fatal ? messages.rmiConnectionLostFatal() : messages.rmiConnectionLostNonfatal());

            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            buttonPanel.setSpacing(5);

            btnExit = new Button(messages.rmiConnectionLostExit());
            btnExit.setEnabled(false);
            btnExit.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    exitAction();
                }
            });
            buttonPanel.add(btnExit);

            btnReconnect = new Button(messages.rmiConnectionLostReconnect());
            btnReconnect.setEnabled(false);
            btnReconnect.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    reconnectAction();
                }
            });
            if (showReconnect) {
                buttonPanel.add(btnReconnect);
            }

            btnRelogin = new Button(messages.rmiConnectionLostRelogin());
            btnRelogin.setEnabled(false);
            btnRelogin.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    reloginAction();
                }
            });
            buttonPanel.add(btnRelogin);

            setModal(true);

            setText(messages.rmiConnectionLost());

            loadingPanel = new VerticalPanel();
            loadingPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            loadingPanel.add(new Image(GWT.getModuleBaseURL() + "images/loading_bar.gif"));

            warningPanel = new VerticalPanel();
            warningPanel.setSpacing(10);
            warningPanel.add(new Image(GWT.getModuleBaseURL() + "images/warning.png"));
            if(fatal)
                warningPanel.setVisible(false);
            errorPanel = new VerticalPanel();
            errorPanel.setSpacing(10);
            errorPanel.add(new Image(GWT.getModuleBaseURL() + "images/error.png"));
            if(!fatal)
                errorPanel.setVisible(false);

            HorizontalPanel centralPanel = new HorizontalPanel();
            centralPanel.add(warningPanel);
            centralPanel.add(errorPanel);
            centralPanel.add(lbMessage);

            VerticalPanel mainPanel = new VerticalPanel();
            mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            if(!fatal)
                mainPanel.add(loadingPanel);
            mainPanel.add(centralPanel);
            mainPanel.add(buttonPanel);

            setWidget(mainPanel);
        }

        public void showDialog() {
            showButtonsTimer = new Timer() {
                @Override
                public void run() {
                    btnExit.setEnabled(true);
                    btnRelogin.setEnabled(true);
                    btnReconnect.setEnabled(true);
                }
            };
            showButtonsTimer.schedule(5000);

            if(!isShowing())
                center();
            makeMaskVisible(true);
        }

        public void hideDialog() {
            if(showButtonsTimer != null)
                showButtonsTimer.cancel();
            hide();
            blockDialog.makeMaskVisible(false);
        }

        private void exitAction() {
            GwtClientUtils.logout();
        }

        private void reconnectAction() {
            Window.Location.reload();
        }

        private void reloginAction() {
            GwtClientUtils.relogin();
        }

        public void setFatal(boolean fatal) {
            if (this.fatal != fatal) {
                lbMessage.setHTML(fatal ? messages.rmiConnectionLostFatal() : messages.rmiConnectionLostNonfatal());
                loadingPanel.setVisible(!fatal);
                warningPanel.setVisible(!fatal);
                errorPanel.setVisible(fatal);
                this.fatal = fatal;
                center();
            }
        }

        public void makeMaskVisible(boolean visible) {
            getElement().getStyle().setOpacity(visible ? 1 : 0);
            getGlassElement().getStyle().setOpacity(visible ? 0.3 : 0);
        }
    }
}