package lsfusion.server.logics.scripted;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.debug.ActionDelegationType;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.ExplicitActionProperty;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

// !!! ONLY ACTIONS CREATED WITH CUSTOM OPERATOR !!!!
public abstract class ScriptingActionProperty extends ExplicitActionProperty {
    protected ScriptingLogicsModule LM;
    
    protected LCP<?> is(ValueClass valueClass) {
        return LM.is(valueClass);
    }

    protected LCP<?> object(ValueClass valueClass) {
        return LM.object(valueClass);
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM) {
        this(LM, new ValueClass[]{});
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(classes);
        this.LM = LM;
    }

    public ScriptingActionProperty(ScriptingLogicsModule LM, LocalizedString caption, ValueClass... classes) {
        super(caption, classes);
        this.LM = LM;
    }

    protected LCP<?> findProperty(String name) throws ScriptingModuleErrorLog.SemanticError {
        return LM.findProperty(name);
    }

    protected LCP<?>[] findProperties(String... names) throws ScriptingModuleErrorLog.SemanticError {
        LCP<?>[] result = new LCP[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = findProperty(names[i]);
        }
        return result;
    }

    //этот метод нужен для дебаггера, чтобы была общая точка для дебаггинга всех executeCustom
    public void commonExecuteCustomDelegate(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        executeCustom(context);
    }

    protected LAP<?> findAction(String name) throws ScriptingModuleErrorLog.SemanticError {
        return LM.findAction(name);
    }

    protected ValueClass findClass(String name) throws ScriptingModuleErrorLog.SemanticError {
        return LM.findClass(name);
    }

    protected AbstractGroup findGroup(String name) throws ScriptingModuleErrorLog.SemanticError {
        return LM.findGroup(name);
    }

    protected FormEntity findForm(String name) throws ScriptingModuleErrorLog.SemanticError {
        return LM.findForm(name);
    }

    protected boolean applySession(ExecutionContext context, DataSession session) throws SQLException, SQLHandledException {
        return session.apply(context);
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE;
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }

    protected boolean isGlobalChange() {
        return true;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.CHANGE && isGlobalChange())
            return true;
        return super.hasFlow(type);
    }

    @Override
    protected boolean allowNulls() { // does not allow by default
        return false;
    }
}
