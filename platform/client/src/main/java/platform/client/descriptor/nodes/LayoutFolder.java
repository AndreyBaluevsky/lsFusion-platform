package platform.client.descriptor.nodes;

import platform.client.logics.ClientForm;

import javax.swing.tree.DefaultMutableTreeNode;

public class LayoutFolder extends DefaultMutableTreeNode {

    public LayoutFolder(ClientForm clientForm) {
        super("Расположение");

        add(new ContainerNode(clientForm.getMainContainer(), clientForm.containers));
    }
}
