package lsfusion.server.form.entity.drilldown;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.CaseUnionProperty;
import lsfusion.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class CaseUnionDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<CaseUnionProperty.Interface, CaseUnionProperty> {

    protected List<PropertyDrawEntity> propProperties;
    protected List<PropertyDrawEntity> whereProperties;
    protected PropertyDrawEntity implPropertyDraw;

    public CaseUnionDrillDownFormEntity(String sID, String caption, CaseUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }

    @Override
    protected void setupDrillDownForm() {
        propProperties = new ArrayList<PropertyDrawEntity>();
        whereProperties = new ArrayList<PropertyDrawEntity>();

        ImList<CaseUnionProperty.Case> cases = property.getCases();

        for (int i = 0; i < cases.size(); ++i) {
            CalcPropertyInterfaceImplement<CaseUnionProperty.Interface> intImpl = cases.get(i).property;
            if (intImpl instanceof CalcPropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface>) intImpl;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !BL.LM.recognizeGroup.hasChild(mapImplement.property)) {
                    if (mapImplement.property.isFull()) {
                        propProperties.add(
                                addPropertyDraw(mapImplement.property, mapImplMapping)
                        );
                    }
                }
            }

            intImpl = cases.get(i).where;
            if (intImpl instanceof CalcPropertyMapImplement) {

                //добавляем фильтр для этого объекта и соотв. свойства
                CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, CaseUnionProperty.Interface>) intImpl;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(interfaceObjects).mapping;

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !BL.LM.recognizeGroup.hasChild(mapImplement.property)) {
                    if (mapImplement.property.isFull()) {
                        whereProperties.add(
                                addPropertyDraw(mapImplement.property, mapImplMapping)
                        );
                    }
                }
            }
        }
        implPropertyDraw = addPropertyDraw(property, interfaceObjects);
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        valueContainer.add(design.get(implPropertyDraw));
        for (int i = propProperties.size()-1; i >= 0; i--) {
            ContainerView propsContainer = design.createContainer(getString("logics.property.drilldown.form.where") + " " + (i + 1));
            propsContainer.constraints.fillHorizontal = 1;
            propsContainer.add(design.get(propProperties.get(i)));
            propsContainer.add(design.get(whereProperties.get(i)));
            design.mainContainer.addAfter(propsContainer, valueContainer);
        }
        return design;
    }
}
