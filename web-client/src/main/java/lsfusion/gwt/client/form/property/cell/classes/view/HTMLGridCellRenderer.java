package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class HTMLGridCellRenderer extends AbstractGridCellRenderer {

    public HTMLGridCellRenderer() {
        super();
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        updateElement(cellElement, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        updateElement(cellElement, value);
    }

    protected void updateElement(DivElement div, Object value) {
        div.setInnerHTML("<iframe src=\"" + value + "\" style=\"width:100%; height:100%;\" >Unfortunately this content could not be displayed</iframe>");
    }
}