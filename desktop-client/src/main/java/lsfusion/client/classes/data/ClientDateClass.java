package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.DatePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.DatePropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.awt.*;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static lsfusion.base.DateConverter.createDateEditFormat;
import static lsfusion.base.DateConverter.safeDateToSql;
import static lsfusion.client.form.property.cell.EditBindingMap.EditEventFilter;

public class ClientDateClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    public final static ClientDateClass instance = new ClientDateClass();

    public byte getTypeId() {
        return DataType.DATE;
    }

    @Override
    protected Object getDefaultWidthValue() {
        return MainFrame.wideFormattableDate;
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        int result = super.getFullWidthString(widthString, fontMetrics, propertyDraw);
        if(propertyDraw.isEditableChangeAction()) // добавляем кнопку если не readonly
            result += 21;
        return result;
    }

    public Format getDefaultFormat() {
        return MainFrame.dateFormat;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DatePropertyRenderer(property);
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format) {
        return createDateEditFormat((DateFormat) format);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DatePropertyEditor(value, getEditFormat(property), property.design);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return safeDateToSql(MainFrame.dateFormat.parse(s));
        } catch (Exception e) {
            throw new ParseException(s +  ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        if (obj != null) {
            return MainFrame.dateFormat.format(obj);
        }
        else return "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
