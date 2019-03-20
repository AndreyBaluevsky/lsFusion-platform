package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.classes.editor.PropertyEditor;
import lsfusion.client.form.property.classes.renderer.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.DateTimePropertyEditor;
import lsfusion.client.form.property.classes.renderer.DateTimePropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.property.DataType;

import java.awt.*;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static lsfusion.base.DateConverter.createDateTimeEditFormat;
import static lsfusion.base.DateConverter.dateToStamp;
import static lsfusion.client.form.property.edit.EditBindingMap.EditEventFilter;

public class ClientDateTimeClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {
    public final static ClientDateTimeClass instance = new ClientDateTimeClass();

    public byte getTypeId() {
        return DataType.DATETIME;
    }

    @Override
    protected Object getDefaultWidthValue() {
        return MainFrame.wideFormattableDateTime;
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        int result = super.getFullWidthString(widthString, fontMetrics, propertyDraw);
        if(propertyDraw.isEditableChangeAction()) // добавляем кнопку если не readonly
            result += 21;
        return result;
    }

    public Format getDefaultFormat() {
        return MainFrame.dateTimeFormat;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DateTimePropertyRenderer(property);
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format) {
        return createDateTimeEditFormat((DateFormat) format);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DateTimePropertyEditor(value, getEditFormat(property), property.design);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return dateToStamp((Date) MainFrame.dateTimeFormat.parseObject(s));
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        if (obj != null) {
            return MainFrame.dateTimeFormat.format(obj);
        }
        else return "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date.with.time");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}