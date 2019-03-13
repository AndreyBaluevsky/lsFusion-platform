package lsfusion.server.logics.tasks.impl;

import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.Property;

public class SetupPropertyPolicyFormsTask extends SetupActionOrPropertyPolicyFormsTask {

    public String getCaption() {
        return "Setup property policy";
    }

    @Override
    protected FormEntity getForm() {
        return getBL().securityLM.propertyPolicyForm;
    }

    @Override
    protected LCP getCanonicalName() {
        return getBL().reflectionLM.propertyCanonicalName;
    }

    @Override
    protected void runTask(Property property) {
        if(property instanceof CalcProperty)
            getBL().setupPropertyPolicyForms(setupPolicyByCN, property, false);
    }
}
