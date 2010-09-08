package platform.client.form.grid;

import platform.client.logics.*;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GridTableModel extends AbstractTableModel {
    private Object[][] data = new Object[0][0];
    private ClientCell[] columnProps = new ClientCell[0];
    private ClientGroupObjectValue[] columnKeys = new ClientGroupObjectValue[0];
    private String[] columnNames = new String[0];

    public void update(List<ClientCell> columnProperties,
                             List<ClientGroupObjectValue> rowKeys,
                             Map<ClientPropertyDraw, List<ClientGroupObjectValue>> mapColumnKeys,
                             Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> columnDisplayValues,
                             Map<ClientCell, Map<ClientGroupObjectValue, Object>> values) {

        List<ClientCell> columnPropsList = new ArrayList<ClientCell>();
        List<ClientGroupObjectValue> columnKeysList = new ArrayList<ClientGroupObjectValue>();

        for (ClientCell cell : columnProperties) {
            //noinspection SuspiciousMethodCalls
            if (cell instanceof ClientPropertyDraw && mapColumnKeys.containsKey(cell)) {
                ClientPropertyDraw propertyDraw = (ClientPropertyDraw) cell;

                for (ClientGroupObjectValue key : mapColumnKeys.get(propertyDraw)) {
                    columnKeysList.add(key);
                    columnPropsList.add(propertyDraw);
                }
            } else {
                columnPropsList.add(cell);
                columnKeysList.add(null);
            }
        }

        if (columnProps.length != columnPropsList.size()) {
            columnProps = new ClientCell[columnPropsList.size()];
            columnKeys = new ClientGroupObjectValue[columnKeysList.size()];
            columnNames = new String[columnKeysList.size()];
        }
        columnProps = columnPropsList.toArray(columnProps);
        columnKeys = columnKeysList.toArray(columnKeys);

        if (data.length==0 || data[0].length != columnProps.length || data.length != rowKeys.size()) {
            data = new Object[rowKeys.size()][columnProps.length];
        }

        for (int i = 0; i < rowKeys.size(); ++i) {
            ClientGroupObjectValue rowKey = rowKeys.get(i);

            for (int j = 0; j < columnProps.length; ++j) {
                ClientGroupObjectValue columnKey = columnKeys[j];
                if (columnKey != null) {
                    for (Map.Entry<ClientGroupObjectValue, Object> entry : values.get(columnProps[j]).entrySet()) {
                        ClientGroupObjectValue key = entry.getKey();
                        if (key.contains(rowKey) && key.contains(columnKey)) {
                            data[i][j] = entry.getValue();
                            break;
                        }
                    }
                } else {
                    data[i][j] = values.get(columnProps[j]).get(rowKey);
                }
            }
        }

        //заполняем имена колонок
        for (int i = 0; i < columnNames.length; ++i) {
            ClientCell cell = columnProps[i];
            ClientGroupObjectValue columnKey = columnKeys[i];

            columnNames[i] = cell.getFullCaption();

            if (cell instanceof ClientPropertyDraw && columnKey != null && !columnKey.isEmpty()) {
                ClientPropertyDraw property = (ClientPropertyDraw) cell;
                String paramCaption = "";
                Iterator<Map.Entry<ClientObject, Object>> columnKeysIt = columnKey.entrySet().iterator();
                for (int j = 0; j < property.columnGroupObjects.length; ++j) {
                    ClientPropertyDraw columnProperty = property.columnDisplayProperties[j];
                    ClientGroupObject columnGroupObject = property.columnGroupObjects[j];

                    ClientGroupObjectValue partColumnKey = new ClientGroupObjectValue();
                    for (int k = 0; k < columnGroupObject.size(); ++k) {
                        Map.Entry<ClientObject, Object> entry = columnKeysIt.next();
                        partColumnKey.put(entry.getKey(), entry.getValue());
                    }
                    if (paramCaption.length() != 0) {
                        paramCaption += ", ";
                    }
                    paramCaption += columnDisplayValues.get(columnProperty).get(partColumnKey);
                }

                columnNames[i] += "[" + paramCaption + "]";
            }
        }
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public int getRowCount() {
        return data.length;
    }

    public int getColumnCount() {
        return columnProps.length;
    }

    public boolean isCellEditable(int row, int column) {
        return !isCellReadOnly(row, column);
    }

    public boolean isCellFocusable(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return false;
        }

        Boolean focusable = columnProps[col].focusable;
        return focusable == null || focusable;
    }

    private boolean isCellReadOnly(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return false;
        }

        Boolean readOnly = columnProps[col].readOnly;
        return readOnly == null || readOnly;
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public void setValueAt(Object value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }

    public int getMinPropertyIndex(ClientCell property) {
        for (int i = 0; i < columnProps.length; ++i) {
            if (property == columnProps[i]) {
                return i;
            }
        }
        return -1;
    }

    public ClientCell getColumnProperty(int index) {
        return columnProps[index];
    }

    public ClientGroupObjectValue getColumnKey(int index) {
        return columnKeys[index];
    }
}
