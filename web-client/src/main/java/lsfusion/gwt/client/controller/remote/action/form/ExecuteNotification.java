package lsfusion.gwt.client.controller.remote.action.form;

public class ExecuteNotification extends FormRequestCountingAction<ServerResponseResult> {
    public Integer idNotification;

    @SuppressWarnings("UnusedDeclaration")
    public ExecuteNotification() {
    }

    public ExecuteNotification(Integer idNotification) {
        this.idNotification = idNotification;
    }
}