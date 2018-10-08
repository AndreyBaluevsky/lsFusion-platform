package lsfusion.server.logics.property.actions.integration.exporting.hierarchy.xml;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.integration.hierarchy.xml.XMLNode;
import lsfusion.server.logics.property.actions.integration.exporting.hierarchy.ExportHierarchicalActionProperty;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.PrintWriter;

public class ExportXMLActionProperty<O extends ObjectSelector> extends ExportHierarchicalActionProperty<XMLNode, O> {
    
    public ExportXMLActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, LCP exportFile, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, exportFile, charset);
    }

    protected XMLNode createRootNode() {
        return new XMLNode(new Element(form.getStaticForm().getName()));
    }

    @Override
    protected void writeRootNode(PrintWriter printWriter, XMLNode rootNode) throws IOException {
        Element element = rootNode.element;
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat().setEncoding(charset));
        xmlOutput.output(new Document(element), printWriter);
    }
}