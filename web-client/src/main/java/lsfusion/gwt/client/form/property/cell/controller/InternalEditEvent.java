package lsfusion.gwt.client.form.property.cell.controller;

public class InternalEditEvent extends EditEvent {
    private final String action;

    public InternalEditEvent(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    @Override
    public void stopPropagation() {
        //do nothing
    }
}
