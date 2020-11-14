/*
 * Copyright 2020 Arthur Sadykov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.isarthur.netbeans.editor.generator.java.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import javax.lang.model.element.TypeElement;
import javax.swing.DefaultCellEditor;
import javax.swing.InputVerifier;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ui.TypeElementFinder;
import org.openide.DialogDescriptor;
import org.openide.awt.Mnemonics;

/**
 *
 * @author Arthur Sadykov
 */
public class GenerateFieldsPanel extends javax.swing.JPanel {

    private static final String[] ACCESS_MODIFIERS = {
        "package", //NOI18N
        "private", //NOI18N
        "protected", //NOI18N
        "public" //NOI18N
    };
    private static final String EMPTY_STRING = ""; //NOI18N
    private DialogDescriptor dialogDescriptor;
    private final DefaultTableModel fieldsTableModel;

    /**
     * Creates new form AddFieldPanel
     */
    private GenerateFieldsPanel() {
        initComponents();
        fieldsTable.getSelectionModel().addListSelectionListener(event -> {
            ListSelectionModel selectionModel = (ListSelectionModel) event.getSource();
            removeFieldButton.setEnabled(!selectionModel.isSelectionEmpty());
        });
        fieldsTableModel = (DefaultTableModel) fieldsTable.getModel();
    }

    public static GenerateFieldsPanel create() {
        return new GenerateFieldsPanel();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addFieldButton = new javax.swing.JButton();
        fieldsScrollPane = new javax.swing.JScrollPane();
        fieldsTable = new javax.swing.JTable();
        removeFieldButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(addFieldButton, org.openide.util.NbBundle.getMessage(GenerateFieldsPanel.class, "GenerateFieldsPanel.addFieldButton.text")); // NOI18N
        addFieldButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFieldButtonActionPerformed(evt);
            }
        });

        fieldsTable.setRowHeight(30);
        fieldsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "access", "static", "final", "transient", "volatile", "type", "name", "value"
            }
        ) {
            Class<?>[] types = new Class<?>[] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Object.class, java.lang.String.class, java.lang.Object.class
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        fieldsTable.setEnabled(false);
        TableColumn accessColumn = fieldsTable.getColumnModel().getColumn(0);
        JComboBox<String> accessComboBox = new JComboBox<>(ACCESS_MODIFIERS);
        accessColumn.setCellEditor(new DefaultCellEditor(accessComboBox));
        accessColumn.setPreferredWidth(50);
        TableColumn staticColumn = fieldsTable.getColumnModel().getColumn(1);
        staticColumn.setPreferredWidth(50);
        TableColumn finalColumn = fieldsTable.getColumnModel().getColumn(2);
        finalColumn.setPreferredWidth(50);
        TableColumn transitiveColumn = fieldsTable.getColumnModel().getColumn(3);
        transitiveColumn.setPreferredWidth(75);
        TableColumn volatileColumn = fieldsTable.getColumnModel().getColumn(4);
        volatileColumn.setPreferredWidth(50);
        TableColumn typeColumn = fieldsTable.getColumnModel().getColumn(5);
        typeColumn.setCellEditor(new TypeEditor());
        typeColumn.setCellRenderer(new TypeRenderer());
        typeColumn.setPreferredWidth(350);
        TableColumn nameColumn = fieldsTable.getColumnModel().getColumn(6);
        JTextField nameTextField = new JTextField();
        nameColumn.setCellEditor (new DefaultCellEditor(nameTextField) {
            private final Border originalBorder = nameTextField.getBorder();

            @Override
            public boolean stopCellEditing() {
                InputVerifier verifier = new NameVerifier();
                if (!(verifier.verify(nameTextField))) {
                    nameTextField.setBorder(new LineBorder(Color.red));
                    nameTextField.selectAll();
                    nameTextField.requestFocusInWindow();
                    dialogDescriptor.setValid(false);
                    return false;
                }
                nameTextField.setBorder(originalBorder);
                boolean result = super.stopCellEditing();
                dialogDescriptor.setValid(valid());
                return result;
            }
        });
        nameColumn.setPreferredWidth(200);
        TableColumn valueColumn = fieldsTable.getColumnModel().getColumn(7);
        valueColumn.setPreferredWidth(200);
        fieldsScrollPane.setViewportView(fieldsTable);

        org.openide.awt.Mnemonics.setLocalizedText(removeFieldButton, org.openide.util.NbBundle.getMessage(GenerateFieldsPanel.class, "GenerateFieldsPanel.removeFieldButton.text")); // NOI18N
        removeFieldButton.setEnabled(false);
        removeFieldButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeFieldButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addFieldButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeFieldButton)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(fieldsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 988, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addFieldButton, removeFieldButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fieldsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addFieldButton)
                    .addComponent(removeFieldButton))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addFieldButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFieldButtonActionPerformed
        addRowToTable();
        dialogDescriptor.setValid(false);
        fieldsTable.setEnabled(!isFieldsTableEmpty());
    }//GEN-LAST:event_addFieldButtonActionPerformed

    private void removeFieldButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeFieldButtonActionPerformed
        removeRowFromTable();
        dialogDescriptor.setValid(valid());
        fieldsTable.setEnabled(!isFieldsTableEmpty());
    }//GEN-LAST:event_removeFieldButtonActionPerformed

    private void addRowToTable() {
        fieldsTableModel.addRow(new Object[]{
            ACCESS_MODIFIERS[1],
            false,
            false,
            false,
            false,
            EMPTY_STRING,
            EMPTY_STRING,
            EMPTY_STRING
        });
        int row = fieldsTable.convertRowIndexToView(fieldsTableModel.getRowCount() - 1);
        fieldsTable.getSelectionModel().setSelectionInterval(row, row);
    }

    private void removeRowFromTable() {
        int row = fieldsTable.convertRowIndexToModel(fieldsTable.getSelectedRow());
        fieldsTableModel.removeRow(row);
        if (fieldsTableModel.getRowCount() > 0) {
            if (row >= 1) {
                row = fieldsTable.convertRowIndexToView(row - 1);
            } else if (row == 0) {
                row = fieldsTable.convertRowIndexToView(row);
            }
            fieldsTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    private boolean isFieldsTableEmpty() {
        return fieldsTableModel.getRowCount() == 0;
    }

    private boolean valid() {
        int rowCount = fieldsTableModel.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            String type = (String) fieldsTableModel.getValueAt(row, 5);
            if (!type.matches("^[a-zA-Z_]\\w*?(\\.[a-zA-Z_]\\w*?)*?$")) { //NOI18N
                return false;
            }
            String name = (String) fieldsTableModel.getValueAt(row, 6);
            if (!name.matches("^[a-zA-Z_]\\w*?$")) { //NOI18N
                return false;
            }
        }
        return true;
    }

    List<?> getData() {
        return fieldsTableModel.getDataVector();
    }

    void setDialogDescriptor(DialogDescriptor dialogDescriptor) {
        this.dialogDescriptor = dialogDescriptor;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addFieldButton;
    private javax.swing.JScrollPane fieldsScrollPane;
    private javax.swing.JTable fieldsTable;
    private javax.swing.JButton removeFieldButton;
    // End of variables declaration//GEN-END:variables
    private class TypePanel extends javax.swing.JPanel {

        /**
         * Creates new form TypePanel
         */
        private TypePanel() {
            initComponents();
            browseButton.addActionListener(this::browseButtonActionPerformed);
        }

        private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {
            ElementHandle<TypeElement> handle = TypeElementFinder.find(null, null, null);
            if (handle != null) {
                typeTextField.setText(handle.getQualifiedName());
            }
        }

        void updateTypeValue(String type) {
            typeTextField.setText(type);
        }

        /**
         * This method is called from within the constructor to
         * initialize the form.
         * WARNING: Do NOT modify this code. The content of this method is
         * always regenerated by the Form Editor.
         */
        @SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
        private void initComponents() {
            typeTextField = new javax.swing.JTextField();
            browseButton = new javax.swing.JButton();
            Mnemonics.setLocalizedText(
                    browseButton, org.openide.util.NbBundle.getMessage(TypePanel.class, "TypePanel.browseButton.text")); // NOI18N
            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
            this.setLayout(layout);
            layout.setHorizontalGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(typeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(browseButton)
                                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
            layout.setVerticalGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(typeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(browseButton))
                                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
        }// </editor-fold>                        
        // Variables declaration - do not modify                     
        private javax.swing.JButton browseButton;
        protected javax.swing.JTextField typeTextField;
        // End of variables declaration                   
    }

    private class TypeRenderer extends TypePanel implements TableCellRenderer {

        private TypeRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            updateTypeValue((String) value);
            return this;
        }
    }

    private class TypeEditor extends TypePanel implements TableCellEditor {

        protected transient ChangeEvent changeEvent;
        private final Border originalBorder = typeTextField.getBorder();

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            this.setBackground(table.getSelectionBackground());
            updateTypeValue((String) value);
            return this;
        }

        @Override
        public Object getCellEditorValue() {
            return typeTextField.getText();
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            InputVerifier verifier = new TypeVerifier();
            if (!(verifier.verify(typeTextField))) {
                typeTextField.setBorder(new LineBorder(Color.red));
                typeTextField.selectAll();
                typeTextField.requestFocusInWindow();
                fireEditingStopped();
                dialogDescriptor.setValid(false);
                return false;
            }
            typeTextField.setBorder(originalBorder);
            fireEditingStopped();
            dialogDescriptor.setValid(valid());
            return true;
        }

        @Override
        public void cancelCellEditing() {
            fireEditingCanceled();
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            listenerList.add(CellEditorListener.class, l);
        }

        @Override public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(CellEditorListener.class, l);
        }

        public CellEditorListener[] getCellEditorListeners() {
            return listenerList.getListeners(CellEditorListener.class);
        }

        protected void fireEditingStopped() {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == CellEditorListener.class) {
                    if (Objects.isNull(changeEvent)) {
                        changeEvent = new ChangeEvent(this);
                    }
                    ((CellEditorListener) listeners[i + 1]).editingStopped(changeEvent);
                }
            }
        }

        protected void fireEditingCanceled() {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == CellEditorListener.class) {
                    // Lazily create the event:
                    if (Objects.isNull(changeEvent)) {
                        changeEvent = new ChangeEvent(this);
                    }
                    ((CellEditorListener) listeners[i + 1]).editingCanceled(changeEvent);
                }
            }
        }
    }

    private class TypeVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
            String text = ((JTextField) input).getText();
            return text.matches("^[a-zA-Z_]\\w*?(\\.[a-zA-Z_]\\w*?)*?$"); //NOI18N
        }
    }

    private class NameVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
            String text = ((JTextField) input).getText();
            return text.matches("^[a-zA-Z_]\\w*?$"); //NOI18N
        }
    }
}
