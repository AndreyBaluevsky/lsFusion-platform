package lsfusion.server.logics.form.open.interactive;

import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.FormCloseType;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.action.input.RequestResult;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.property.checked.PullChangeProperty;
import lsfusion.server.logics.form.open.FormAction;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilter;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyValueImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Function;

public class FormInteractiveAction<O extends ObjectSelector> extends FormAction<O> {

    // INPUT
    private final ImList<O> inputObjects;
    private final ImList<LP> inputProps;    
    private final ImList<Boolean> inputNulls;

    private final Boolean syncType;
    private final WindowFormType windowType;
    
    private ModalityType getModalityType(boolean syncType) {
        WindowFormType windowType = this.windowType;
        if(windowType == null) {
            if(syncType)
                windowType = WindowFormType.FLOAT;
            else
                windowType = WindowFormType.DOCKED;
        }

        if(syncType) {
            if (windowType == WindowFormType.FLOAT) {
                if(!inputObjects.isEmpty())
                    return ModalityType.DIALOG_MODAL;
                return ModalityType.MODAL;
            } else {
                assert (windowType == WindowFormType.DOCKED);
                return ModalityType.DOCKED_MODAL;
            }
        } else
            return ModalityType.DOCKED;
    }

    // SYSTEM ACTIONS
    private final ManageSessionType manageSession;
    private final Boolean noCancel;

    // NAVIGATOR
    private final Boolean forbidDuplicate;

    // CONTEXT
    private final ImList<O> contextObjects;
    private final ImList<PropertyMapImplement<PropertyInterface, ClassPropertyInterface>> contextPropertyImplements;
    private final boolean readOnly;
    private final boolean checkOnOk;

    public FormInteractiveAction(LocalizedString caption,
                                 FormSelector<O> form,
                                 final ImList<O> objectsToSet, final ImList<Boolean> nulls,
                                 ImList<O> inputObjects, ImList<LP> inputProps, ImList<Boolean> inputNulls,
                                 ImList<O> contextObjects, ImList<Property> contextProperties,
                                 ManageSessionType manageSession,
                                 Boolean noCancel,
                                 Boolean syncType,
                                 WindowFormType windowType, boolean forbidDuplicate,
                                 boolean checkOnOk,
                                 boolean readOnly) {
        super(caption, form, objectsToSet, nulls, true, new ValueClass[] {}, contextProperties.toArray(new Property[contextProperties.size()]));

        this.inputObjects = inputObjects;
        this.inputProps = inputProps;
        this.inputNulls = inputNulls;

        this.syncType = syncType;
        this.windowType = windowType;

        this.forbidDuplicate = forbidDuplicate;

        this.manageSession = manageSession;
        this.noCancel = noCancel;

        this.contextObjects = contextObjects;
        this.contextPropertyImplements = contextProperties.mapListValues((Function<Property, PropertyMapImplement<PropertyInterface, ClassPropertyInterface>>) value -> value.getImplement(
                getOrderInterfaces().subOrder(objectsToSet.size(), interfaces.size())
        ));
        
        this.readOnly = readOnly;
        this.checkOnOk = checkOnOk;
    }
    
    private boolean isShowDrop() {
        for(Boolean inputNull : inputNulls)
            if (inputNull)
                return true;
        return false;
    }

    protected boolean isSync() {
        return true;
    }

    private boolean heuristicSyncType(ExecutionContext<ClassPropertyInterface> context) {
        FormInstance formInstance;
        return context.hasMoreSessionUsages || ((formInstance = context.getFormInstance(false, false)) != null && formInstance.isFloat());
    }

    @Override
    protected void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException {
        ImRevMap<O, ObjectEntity> mapRevResolvedObjects = mapResolvedObjects.reverse();

        Result<ImSet<PullChangeProperty>> pullProps = new Result<>();
        ImSet<ContextFilter> contextFilters = getContextFilters(context, pullProps, mapRevResolvedObjects);

        boolean syncType; 
        if(this.syncType != null)
            syncType = this.syncType;
        else
            syncType = heuristicSyncType(context);
        ModalityType modalityType = getModalityType(syncType);

        FormInstance newFormInstance = context.createFormInstance(form, mapObjectValues, context.getSession(), syncType, noCancel, manageSession, checkOnOk, isShowDrop(), true, modalityType.isModal(), contextFilters, pullProps.result, readOnly);
        context.requestFormUserInteraction(newFormInstance, modalityType, forbidDuplicate, context.stack);

        if (syncType) {
            FormCloseType formResult = newFormInstance.getFormResult();

            ImList<RequestResult> result = null;
            if(formResult != FormCloseType.CLOSE) {
                ImList<ObjectEntity> resolvedInputObjects = inputObjects.mapList(mapRevResolvedObjects);
                MList<RequestResult> mResult = ListFact.mList(resolvedInputObjects.size());
                for (int i = 0; i < resolvedInputObjects.size(); i++) {
                    ObjectEntity inputObject = resolvedInputObjects.get(i);
                    ObjectInstance object = newFormInstance.instanceFactory.getInstance(inputObject);

                    ObjectValue chosenValue = (formResult == FormCloseType.OK ? object.getObjectValue() : NullValue.instance);
                    mResult.add(new RequestResult(chosenValue, object.getType(), inputProps.get(i)));
                }
                result = mResult.immutableList();
            }
            context.writeRequested(result);
        }
    }

    private ImSet<ContextFilter> getContextFilters(ExecutionContext<ClassPropertyInterface> context, Result<ImSet<PullChangeProperty>> pullProps, ImRevMap<O, ObjectEntity> mapResolvedObjects) {
        MSet<ContextFilter> mContextFilters = SetFact.mSet();
        ImList<ObjectEntity> resolvedContextObjects = contextObjects.mapList(mapResolvedObjects);
        for(int i=0,size=resolvedContextObjects.size();i<size;i++) {
            ObjectEntity contextObject = resolvedContextObjects.get(i);
            PropertyMapImplement<PropertyInterface, ClassPropertyInterface> contextPropertyImplement = contextPropertyImplements.get(i);
            
            final PropertyValueImplement<PropertyInterface> propertyValues = contextPropertyImplement.mapObjectValues(context.getKeys());
            assert propertyValues != null; // extraNotNull - true
            final FormInstance thisFormInstance = context.getFormInstance(false, true);
            mContextFilters.addAll(thisFormInstance.getContextFilters(contextObject, propertyValues, context.getChangingPropertyToDraw(), pullProps));
        }
        return mContextFilters.immutable();
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.FORMCHANGE) {
            if(!readOnly) { // если не read only
                FormEntity staticForm = form.getStaticForm();
                if(staticForm == null || !staticForm.hasNoChange()) // и форма не известна и может что-то изменять
                    return true;
            }
        }
        if(type == ChangeFlowType.NEEDMORESESSIONUSAGES && syncType == null)
            return true;
        return super.hasFlow(type);
    }
}
