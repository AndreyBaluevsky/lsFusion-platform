package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class TextGridCellRenderer extends TextBasedGridCellRenderer {
    private final boolean rich;

    public TextGridCellRenderer(GPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        divStyle.setProperty("lineHeight", "normal");
        if (!rich) {
            divStyle.setProperty("wordWrap", "break-word");
            divStyle.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
        }

        GFont font = property.font;
        if (font == null && table instanceof GGridPropertyTable) {
            font = ((GGridPropertyTable) table).font;
        }
        if (font != null) {
            font.apply(divStyle);
        }

        updateElement(cellElement, value);
    }

    @Override
    protected void updateElement(DivElement div, Object value) {
        if (!rich || value == null) {
            super.updateElement(div, value);
        } else {
            div.removeClassName("nullValueString");
            div.getStyle().setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
            div.setInnerHTML(EscapeUtils.sanitizeHtml((String) value));
        }
    }

    @Override
    protected String renderToString(Object value) {
        return (String) value;
    }
}