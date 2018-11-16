package lsfusion.server.form.view.report;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.FontInfo;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.ReportConstants;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.PropertyReaderEntity;
import lsfusion.server.form.stat.StaticDataGenerator;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.form.stat.FormReportInterface;
import lsfusion.server.session.DataSession;
import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;

import java.sql.SQLException;
import java.util.*;

import static lsfusion.interop.form.ReportConstants.*;
import static lsfusion.server.form.entity.GroupObjectHierarchy.ReportNode;

public class ReportDesignGenerator {
    private StaticDataGenerator.ReportHierarchy hierarchy;
    private FormReportInterface formInterface;
    private FormView formView;

    private MAddExclMap<PropertyDrawEntity, ImMap<ImMap<ObjectEntity, Object>, ImOrderSet<ImMap<ObjectEntity, Object>>>> columnGroupObjects;
    private int getPropGroupColumnsCount(PropertyDrawEntity property) {
        if(columnGroupObjects == null)
            return 1;

        int maxColumns = 0;
        for(ImOrderSet<ImMap<ObjectEntity, Object>> row : columnGroupObjects.get(property).valueIt())
            maxColumns = BaseUtils.max(maxColumns, row.size());
        return maxColumns;
    }    
            
    private static final int defaultPageWidth = 842;  //
    private static final int defaultPageHeight = 595; // эти константы есть в JasperReports Ultimate Guide

    private static final int defaultPageMargin = 20;

    private int pageWidth;
    private int pageUsableWidth;
    private static final int neighboursGap = 5;

    private int rowHeight = 18;
    private int charWidth = 8;
    private boolean toStretch = true;
    
    private Map<GroupObjectEntity, ImList<ReportDrawField>> groupFields; // optimization
    
    private Map<ReportNode, JasperDesign> designs = new HashMap<>();

    public ReportDesignGenerator(FormView formView, StaticDataGenerator.ReportHierarchy hierarchy, FormPrintType printType, MAddExclMap<PropertyDrawEntity, ImMap<ImMap<ObjectEntity, Object>, ImOrderSet<ImMap<ObjectEntity, Object>>>> columnGroupObjects, MAddExclMap<PropertyReaderEntity, Type> types, FormReportInterface formInterface) {
        this.formView = formView;
        this.hierarchy = hierarchy;
        this.formInterface = formInterface;
        
        this.columnGroupObjects = columnGroupObjects;

        Map<GroupObjectEntity, ImList<ReportDrawField>> groupFields = new HashMap<>();
        for (ReportNode reportNode : hierarchy.reportHierarchy.getAllNodes())
            for (GroupObjectEntity group : reportNode.getGroupList())
                groupFields.put(group, getReportDrawFields(group, hierarchy.hierarchy, types));
        this.groupFields = groupFields;

        pageWidth = calculatePageWidth(printType);
        pageUsableWidth = pageWidth - defaultPageMargin * 2;
    }

    private int calculatePageWidth(FormPrintType printType) {
        if (formView.overridePageWidth != null)
            return formView.overridePageWidth;

        int maxGroupWidth = defaultPageWidth;
        if(printType.ignorePagination()) {
            for (ImList<ReportDrawField> fields : groupFields.values()) {
                maxGroupWidth = Math.max(maxGroupWidth, calculateGroupPreferredWidth(fields));
            }
        }
        return maxGroupWidth;
    }

    public Map<ReportNode, JasperDesign> generate() throws JRException {
        try {
            BaseLogicsModule baseLM = ThreadLocalContext.getBusinessLogics().LM;
            try(DataSession session = ThreadLocalContext.getDbManager().createSession()) {
                charWidth = (Integer) baseLM.reportCharWidth.read(session);
                rowHeight = (Integer) baseLM.reportRowHeight.read(session);
                toStretch = BaseUtils.nvl((Boolean) baseLM.reportToStretch.read(session), false);
            }
        } catch (SQLException | SQLHandledException e) {
            ServerLoggers.systemLogger.warn("Error when reading report parameters", e);
        }

        iterateChildReport(hierarchy.reportHierarchy.rootNode, true, true);

        return designs;
    }

    private JasperDesign iterateChildReport(ReportNode node, boolean isRoot, boolean isFirst) throws JRException {
        JasperDesign design = createJasperDesignObject(node, isRoot, !isFirst);
        createDesignGroups(design, node);        
        iterateChildReports(design, node);
        
        return design;
    }
    
    private void iterateChildReports(JasperDesign design, ReportNode node) throws JRException {
        List<ReportNode> children = hierarchy.reportHierarchy.getChildNodes(node);
        for (ReportNode childNode : children) {
            JasperDesign childDesign = iterateChildReport(childNode, false, (childNode == children.get(0)));

            addSubReportBand(design, childDesign, childNode.getID());
        }
    }

    private void addSubReportBand(JasperDesign design, JasperDesign childDesign, String childSID) throws JRException {
        JRDesignSubreport subReportElement = new JRDesignSubreport(childDesign);
        subReportElement.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
        addSubReportParameters(design, subReportElement, childSID);
        
        JRDesignBand detail = new JRDesignBand();
        detail.addElement(subReportElement);
        ((JRDesignSection)design.getDetailSection()).addBand(detail);
    }

    private ImList<ReportDrawField> getReportDrawFields(GroupObjectEntity group, StaticDataGenerator.Hierarchy hierarchy, final MAddExclMap<PropertyReaderEntity, Type> types) {
        return formInterface.getUserOrder(group, hierarchy.getProperties(group)).mapListValues(new GetValue<ReportDrawField, PropertyDrawEntity>() {
            public ReportDrawField getMapValue(PropertyDrawEntity prop) {
                ReportDrawField reportField = formView.get(prop).getReportDrawField(charWidth, getPropGroupColumnsCount(prop), types != null ? types.get(prop) : prop.getType());

                Integer widthUser = formInterface.getUserWidth(prop);
                if (widthUser != null)
                    reportField.setWidthUser(widthUser);
                return reportField;
            }
        });
    }

    private int calculateGroupPreferredWidth(ImList<ReportDrawField> fields) {
        int width = 0;
        for (ReportDrawField field : fields) {
            width += field.getPreferredWidth();
        }
        return width;
    }

    private void createDesignGroups(final JasperDesign design, ReportNode node) throws JRException {
        List<GroupObjectEntity> groups = node.getGroupList();
        for (GroupObjectEntity group : groups) { // can be null for root report node
            ImList<ReportDrawField> drawFields = groupFields.get(group);

            // adding design fields
            if(group != null)
                for (ObjectEntity object : group.getObjects()) {
                    ReportDrawField objField = new ReportDrawField(object.getSID() + objectSuffix, "", charWidth);
                    object.getType().fillReportDrawField(objField);
                    addDesignField(design, objField);
                }

            boolean isDetail = hierarchy.reportHierarchy.isLeaf(node) && (group == groups.get(groups.size() - 1));
                
            // adding groups
            final JRDesignGroup designGroup;
            if(!isDetail && group != null)
                designGroup = addDesignGroup(design, group, "designGroup");
            else
                designGroup = null;

            // creating styles
            JRDesignStyle groupCellStyle = getGroupCellStype(node, group, groups);
            JRDesignStyle groupCaptionStyle = groupCellStyle;
            design.addStyle(groupCellStyle);

            // adding report fields
            if(!drawFields.isEmpty()) {
                // creating layouts
                ReportLayout reportLayout;
                if (isDetail) { // is detail
                    reportLayout = new ReportDetailLayout(design, rowHeight);
                } else {
                    ReportLayout.BandSection section;
                    if (group != null)
                        section = new ReportLayout.MultipleBandSection() {
                            public void addBand(JRBand band) {
                                ((JRDesignSection) designGroup.getGroupHeaderSection()).addBand(band);
                            }
                        };
                    else section = new ReportLayout.BandSection() {
                        public void setBand(JRBand band) {
                            design.setTitle(band);
                        }
                    };

                    if (section instanceof ReportLayout.MultipleBandSection && !isSimpleBand(drawFields)) {
                        reportLayout = new ReportGroupColumnLayout((ReportLayout.MultipleBandSection) section, rowHeight);
                    } else {
                        reportLayout = new ReportGroupRowLayout(section, rowHeight);
                    }
                }

                for (ReportDrawField reportField : drawFields)
                    addReportFieldToLayout(reportLayout, reportField, groupCaptionStyle, groupCellStyle);

                // layouting
                reportLayout.doLayout(pageUsableWidth);

                for (ReportDrawField propertyField : drawFields) {
                    addDesignField(design, propertyField);
                    addSupplementalDesignFields(design, propertyField);
                }
            }
        }
    }

    // fits pageUsableWidth and does not have column group objects
    private boolean isSimpleBand(ImList<ReportDrawField> drawFields) {
        int totalWidth = 0;
        for (ReportDrawField reportField : drawFields) {
            totalWidth += reportField.getCaptionWidth() + reportField.getPreferredWidth();
            if(totalWidth > pageUsableWidth)
                return false;
            if(reportField.hasColumnGroupObjects)
                return false;
        }
        return true;        
    }

    private JRDesignStyle getGroupCellStype(ReportNode node, GroupObjectEntity group, List<GroupObjectEntity> groups) {
        int groupIndex = groups.indexOf(group);
        JRDesignStyle groupCellStyle = DesignStyles.getGroupStyle(node.getGroupLevel() - groupIndex - 1, hierarchy.reportHierarchy.rootNode.getGroupLevel() - 1);
        
        FontInfo font = getFont(group);
        if (font != null) {
            groupCellStyle.setFontSize((float) font.fontSize);
            groupCellStyle.setBold(font.isBold());
            groupCellStyle.setItalic(font.isItalic());
        }
        return groupCellStyle;
    }

    private FontInfo getFont(GroupObjectEntity group) {
        FontInfo font = formInterface.getUserFont(group);
        if (font == null && group != null) {
            font = formView.get(group).getGrid().design.getFont();
        }
        return font;
    }

    private void addSupplementalDesignFields(JasperDesign design, ReportDrawField field) throws JRException {
        if (field.hasHeaderProperty) {
            String fieldId = field.sID + headerSuffix;
            addDesignField(design, fieldId, field.headerClass.getName());
        }
        if (field.hasFooterProperty) {
            String fieldId = field.sID + footerSuffix;
            addDesignField(design, fieldId, field.footerClass.getName());
        }
        if (field.hasShowIfProperty) {
            String fieldId = field.sID + showIfSuffix;
            addDesignField(design, fieldId, field.showIfClass.getName());
        }
    }
    
    private void addReportFieldToLayout(ReportLayout layout, ReportDrawField reportField, JRDesignStyle captionStyle, JRDesignStyle style) {
        String designCaptionText;
        if (reportField.hasHeaderProperty) {
            designCaptionText = ReportUtils.createFieldString(reportField.sID + headerSuffix);
        } else {
            designCaptionText = '"' + reportField.caption + '"';
        }
        JRDesignExpression captionExpr = ReportUtils.createExpression(designCaptionText, reportField.headerClass);
        JRDesignTextField captionField = ReportUtils.createTextField(captionStyle, captionExpr, toStretch);
        captionField.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
        captionField.setBlankWhenNull(true);
        captionField.setKey(reportField.columnGroupName == null ? null : reportField.columnGroupName + ".caption");

        JRDesignExpression dataExpr = ReportUtils.createExpression(ReportUtils.createFieldString(reportField.sID), reportField.valueClass);
        JRDesignTextField dataField = ReportUtils.createTextField(style, dataExpr, toStretch);
        dataField.setHorizontalAlignment(HorizontalAlignEnum.getByValue(reportField.alignment));
        dataField.setPositionType(PositionTypeEnum.FLOAT);
        dataField.setBlankWhenNull(true);
        dataField.setKey(reportField.columnGroupName);
        dataField.setPattern(reportField.pattern);

        layout.add(reportField, captionField, dataField);
    }

    private JRDesignGroup addDesignGroup(JasperDesign design, GroupObjectEntity group, String groupName) throws JRException {
        
        JRDesignGroup designGroup = new JRDesignGroup();
        
        String groupString = "";
        if(group != null) {
            for (ObjectEntity object : group.getOrderObjects()) {
                groupString = (groupString.length() == 0 ? "" : groupString + "+\" \"+") + "String.valueOf($F{" + object.getSID() + ".object})";
            }
            groupName = groupName + group.getID();
        }

        designGroup.setName(groupName);
        
        JRDesignExpression groupExpr = ReportUtils.createExpression(groupString, java.lang.String.class);
        designGroup.setExpression(groupExpr);

        design.addGroup(designGroup);

        return designGroup;
    }

    private JRDesignField addDesignField(JasperDesign design, ReportDrawField reportField) throws JRException {
        return addDesignField(design, reportField.sID, reportField.valueClass.getName());
    }

    private JRDesignField addDesignField(JasperDesign design, String id, String className) throws JRException {
        JRDesignField designField = ReportUtils.createField(id, className);
        design.addField(designField);
        return designField;
    }

    private static JRDesignExpression createParameterExpression(JasperDesign design, String sid, String suffix, Class cls) throws JRException {
        String parameter = sid + suffix;
        ReportUtils.addParameter(design, parameter, cls);
        return ReportUtils.createExpression(ReportUtils.createParamString(parameter), cls);        
    }
    private static void addSubReportParameters(JasperDesign design, JRDesignSubreport subReportElement, String sid) throws JRException {
        subReportElement.setExpression(createParameterExpression(design, sid, ReportConstants.reportSuffix, JasperReport.class));
        subReportElement.setDataSourceExpression(createParameterExpression(design, sid, ReportConstants.sourceSuffix, JRDataSource.class));
        subReportElement.setParametersMapExpression(createParameterExpression(design, sid, ReportConstants.paramsSuffix, Map.class));
    }

    private JasperDesign createJasperDesignObject(ReportNode node, boolean needMargin, boolean needTopMargin) throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName(node.getName(formView));

        if(!needMargin) {
            design.setTopMargin(needTopMargin ? neighboursGap : 0);
            design.setBottomMargin(0);
            design.setLeftMargin(0);
            design.setRightMargin(0);
        }

        design.setPageWidth(pageWidth);
        design.setPageHeight(defaultPageHeight);

        design.setOrientation(OrientationEnum.LANDSCAPE);

        design.addStyle(DesignStyles.getDefaultStyle());
        designs.put(node, design);
        return design;
    }
}
