package lsfusion.server.logics.form.open.stat;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.StaticFormDataManager;
import lsfusion.server.logics.form.stat.print.PrintMessageData;
import lsfusion.server.logics.form.stat.print.StaticFormReportManager;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrintAction<O extends ObjectSelector> extends FormStaticAction<O, FormPrintType> {
    private final boolean hasPrinter;
    private final boolean hasSheetName;
    private final boolean hasPassword;

    private final LP formPageCount;

    private final boolean syncType; // static interactive
    
    private final boolean removeNullsAndDuplicates; // print message

    protected final LP<?> exportFile; // nullable

    public PrintAction(LocalizedString caption,
                       FormSelector<O> form,
                       final ImList<O> objectsToSet,
                       final ImList<Boolean> nulls,
                       FormPrintType staticType,
                       boolean syncType,
                       Integer top,
                       LP exportFile,
                       LP formPageCount, boolean removeNullsAndDuplicates,
                       boolean hasPrinter, boolean hasSheetName, boolean hasPassword,
                       ValueClass... valueClasses) {
        super(caption, form, objectsToSet, nulls, staticType, top, valueClasses);

        this.hasPrinter = hasPrinter;
        this.hasSheetName = hasSheetName;
        this.hasPassword = hasPassword;

        this.formPageCount = formPageCount;

        this.syncType = syncType;
        
        this.removeNullsAndDuplicates = removeNullsAndDuplicates;
        
        this.exportFile = exportFile;
    }

    @Override
    protected void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException {
        if (staticType == FormPrintType.MESSAGE) {
            // getting data
            PrintMessageData reportData = new StaticFormDataManager(form, mapObjectValues, context).getPrintMessageData(selectTop, removeNullsAndDuplicates);

            // proceeding data
            LogMessageClientAction action = new LogMessageClientAction(reportData.message, reportData.titles, reportData.rows, !context.getSession().isNoCancelInTransaction());
            if(syncType)
                context.requestUserInteraction(action);
            else
                context.delayUserInteraction(action);
        } else {
            // getting data
            StaticFormReportManager formReportManager = new StaticFormReportManager(form, mapObjectValues, context);
            ReportGenerationData reportData = formReportManager.getReportData(staticType, selectTop);

            String sheetName = null;
            if(hasSheetName) {
                ObjectValue sheetNameObject = context.getKeys().get(getOrderInterfaces().get(context.getKeyCount() - (hasPassword ? 2 : 1)));
                sheetName = sheetNameObject instanceof DataObject ? (String) sheetNameObject.getValue() : null;
            }

            String password = null;
            if(hasPassword) {
                ObjectValue passwordObject = context.getKeys().get(getOrderInterfaces().get(context.getKeyCount() - 1));
                password = passwordObject instanceof DataObject ? (String) passwordObject.getValue() : null;
            }

            if (exportFile != null)
                writeResult(exportFile, staticType, context, ReportGenerator.exportToFileByteArray(reportData, staticType, sheetName, password));
            else {
                //printer and sheet/password options doesn't intersect
                String printer = null;
                if(hasPrinter) {
                    ObjectValue printerObject = context.getKeys().get(getOrderInterfaces().get(context.getKeyCount() - 1));
                    printer = printerObject instanceof DataObject ? (String) printerObject.getValue() : null;
                }
                List<ReportPath> customReportPathList = SystemProperties.inDevMode && form.isNamed() && context.getBL().findForm(form.getCanonicalName()) != null ? formReportManager.getCustomReportPathList(staticType) : new ArrayList<>(); // checking that form is not in script, etc.
                Integer pageCount = (Integer) context.requestUserInteraction(new ReportClientAction(customReportPathList, form.getSID(), syncType, reportData, staticType, printer, SystemProperties.inDevMode, password, sheetName));
                formPageCount.change(pageCount, context);
            }
        }
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        if(exportFile != null)
            return getChangeProps(exportFile.property);
        return MapFact.EMPTY();
    }
}
