package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;

public abstract class GFormLayoutImpl {
    private static GFormLayoutImpl impl;

    public static GFormLayoutImpl get() {
        if (impl == null) {
            impl = GWT.create(GFormLayoutImpl.class);
        }
        return impl;
    }

    public abstract GAbstractContainerView createContainerView(GFormController form, GContainer container);

    public abstract void setupMainContainer(Widget mainContainerWidget);

    public abstract Panel createGridView(GGrid grid, ResizableSimplePanel panel);
    
    public abstract Panel createTreeView(GTreeGroup treeGroup, ResizableSimplePanel panel);
}