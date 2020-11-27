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
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;
import javax.swing.DefaultCellEditor;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
public class GenerateMethodPanel extends javax.swing.JPanel implements DocumentListener {

    private static final String EMPTY_STRING = ""; //NOI18N
    private static final String NAME_REGEX = "^[a-zA-Z_]\\w*?$"; //NOI18N
    private static final String TYPE_PARAMETER_NAME_REGEX = "^([a-zA-Z_]\\w*?)|(\\?)$"; //NOI18N
    private static final String TYPE_REGEX =
            "^\\s*?(([a-zA-Z_]\\w*?\\s*?(\\<\\s*?((\\?)|([a-zA-Z_]\\w*?)|((\\?)|([a-zA-Z_?]\\w*?)\\s+?extends\\s+?[a-zA-Z_]" //NOI18N
            + "\\w*?))\\s*?\\>)?)" //NOI18N
            + "|([a-zA-Z_]\\w*?\\s*?\\[\\])" //NOI18N
            + "|([a-zA-Z_]\\w*?(\\s*?\\.[a-zA-Z_]\\w*?)+?\\s*?\\[\\])" //NOI18N
            + "|([a-zA-Z_]\\w*?(\\s*?\\.[a-zA-Z_]\\w*?)+?\\s*?(\\<\\s*?((\\?)|([a-zA-Z_]\\w*?)|((\\?)|([a-zA-Z_?]\\w*?)" //NOI18N
            + "\\s+?extends\\s+?[a-zA-Z_]\\w*?))\\s*?\\>)?))\\s*?$"; //NOI18N
    private static final String TYPE_REGEX_EMPTY_ALLOWED =
            "(^\\s*?(([a-zA-Z_]\\w*?\\s*?(\\<\\s*?((\\?)|([a-zA-Z_]\\w*?)|((\\?)|([a-zA-Z_?]\\w*?)\\s+?extends\\s+?[a-zA-Z_]" //NOI18N
            + "\\w*?))\\s*?\\>)?)" //NOI18N
            + "|([a-zA-Z_]\\w*?\\s*?\\[\\])" //NOI18N
            + "|([a-zA-Z_]\\w*?(\\s*?\\.[a-zA-Z_]\\w*?)+?\\s*?\\[\\])" //NOI18N
            + "|([a-zA-Z_]\\w*?(\\s*?\\.[a-zA-Z_]\\w*?)+?\\s*?(\\<\\s*?((\\?)|([a-zA-Z_]\\w*?)|((\\?)|([a-zA-Z_?]\\w*?)" //NOI18N
            + "\\s+?extends\\s+?[a-zA-Z_]\\w*?))\\s*?\\>)?))\\s*?$)|(^$)"; //NOI18N
    private final DefaultTableModel parametersTableModel;
    private final DefaultTableModel typeParametersTableModel;
    private final DefaultTableModel throwsTableModel;
    private DialogDescriptor dialogDescriptor;

    private GenerateMethodPanel(boolean isClass) {
        initComponents();
        if (!isClass) {
            accessComboBox.setEnabled(false);
            staticCheckBox.setEnabled(false);
            finalCheckBox.setEnabled(false);
            synchronizedCheckBox.setEnabled(false);
            nativeCheckBox.setEnabled(false);
            strictfpCheckBox.setEnabled(false);
        }
        parametersTable.getSelectionModel().addListSelectionListener(event -> {
            ListSelectionModel selectionModel = (ListSelectionModel) event.getSource();
            removeParameterButton.setEnabled(!selectionModel.isSelectionEmpty());
        });
        typeParametersTable.getSelectionModel().addListSelectionListener(event -> {
            ListSelectionModel selectionModel = (ListSelectionModel) event.getSource();
            removeTypeParameterButton.setEnabled(!selectionModel.isSelectionEmpty());
        });
        throwsTable.getSelectionModel().addListSelectionListener(event -> {
            ListSelectionModel selectionModel = (ListSelectionModel) event.getSource();
            removeThrownTypeButton.setEnabled(!selectionModel.isSelectionEmpty());
        });
        accessComboBox.addItemListener(event -> {
            String access = (String) accessComboBox.getSelectedItem();
            if (access != null && access.equals("private")) { //NOI18N
                abstractCheckBox.setSelected(false);
            }
        });
        parametersTableModel = (DefaultTableModel) parametersTable.getModel();
        typeParametersTableModel = (DefaultTableModel) typeParametersTable.getModel();
        throwsTableModel = (DefaultTableModel) throwsTable.getModel();
    }

    public static GenerateMethodPanel create(boolean isClass) {
        GenerateMethodPanel generateMethodPanel = new GenerateMethodPanel(isClass);
        generateMethodPanel.getTypeTextField().getDocument().addDocumentListener(generateMethodPanel);
        generateMethodPanel.getNameTextField().getDocument().addDocumentListener(generateMethodPanel);
        return generateMethodPanel;
    }

    private String suggestParameterName(String type) {
        String name = ""; //NOI18N
        Matcher matcher = Pattern.compile("^([a-zA-Z_]\\w+?)((\\[\\])|(\\<.*?\\>))?$").matcher(type); //NOI18N
        if (matcher.matches()) {
            name = matcher.group(1);
            name = name.substring(0, 1).toLowerCase(Locale.getDefault()).concat(name.substring(1));
        } else {
            matcher = Pattern
                    .compile("^([a-zA-Z_]\\w+?(\\.[a-zA-Z_]\\w+?)+?)((\\[\\])|(\\<.*?\\>))?$") //NOI18N
                    .matcher(type);
            if (matcher.matches()) {
                name = matcher.group(1).substring(matcher.group(1).lastIndexOf('.') + 1);
                name = name.substring(0, 1).toLowerCase(Locale.getDefault()).concat(name.substring(1));
            }
        }
        return name;
    }

    private JTextField getNameTextField() {
        return nameTextField;
    }

    private JTextField getTypeTextField() {
        return typeTextField;
    }

    @Override
    public void insertUpdate(DocumentEvent event) {
        dialogDescriptor.setValid(valid());
    }

    @Override
    public void changedUpdate(DocumentEvent event) {
        dialogDescriptor.setValid(valid());
    }

    @Override
    public void removeUpdate(DocumentEvent event) {
        dialogDescriptor.setValid(valid());
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

        typeLabel = new javax.swing.JLabel();
        typeTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        nameLabel = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        parametersScrollPane = new javax.swing.JScrollPane();
        parametersTable = new javax.swing.JTable();
        addParameterButton = new javax.swing.JButton();
        removeParameterButton = new javax.swing.JButton();
        typeParametersScrollPane = new javax.swing.JScrollPane();
        typeParametersTable = new javax.swing.JTable();
        addTypeParameterButton = new javax.swing.JButton();
        removeTypeParameterButton = new javax.swing.JButton();
        addThrownTypeButton = new javax.swing.JButton();
        removeThrownTypeButton = new javax.swing.JButton();
        throwsScrollPane = new javax.swing.JScrollPane();
        throwsTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        finalCheckBox = new javax.swing.JCheckBox();
        staticCheckBox = new javax.swing.JCheckBox();
        abstractCheckBox = new javax.swing.JCheckBox();
        strictfpCheckBox = new javax.swing.JCheckBox();
        accessComboBox = new javax.swing.JComboBox<>();
        nativeCheckBox = new javax.swing.JCheckBox();
        synchronizedCheckBox = new javax.swing.JCheckBox();
        accessLabel = new javax.swing.JLabel();

        typeLabel.setLabelFor(typeTextField);
        org.openide.awt.Mnemonics.setLocalizedText(typeLabel, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.typeLabel.text")); // NOI18N

        typeTextField.setText(org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.typeTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        nameLabel.setLabelFor(nameTextField);
        org.openide.awt.Mnemonics.setLocalizedText(nameLabel, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.nameLabel.text")); // NOI18N

        nameTextField.setText(org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.nameTextField.text")); // NOI18N

        parametersScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.parametersScrollPane.border.title"), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION)); // NOI18N

        parametersTable.setRowHeight(30);
        parametersTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Final", "Type", "Name"
            }
        ) {
            Class<?>[] types = new Class<?> [] {
                java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        parametersTable.setEnabled(false);
        TableColumn finalColumn = parametersTable.getColumnModel().getColumn(0);
        finalColumn.setPreferredWidth(50);
        TableColumn typeColumn = parametersTable.getColumnModel().getColumn(1);
        typeColumn.setCellEditor(new TypeEditor(false, parametersTable));
        typeColumn.setCellRenderer(new TypeRenderer());
        typeColumn.setPreferredWidth(350);
        TableColumn nameColumn = parametersTable.getColumnModel().getColumn(2);
        JTextField parameterNameTextField = new JTextField();
        nameColumn.setCellEditor (new DefaultCellEditor(parameterNameTextField) {
            private final Border originalBorder = parameterNameTextField.getBorder();

            @Override
            public boolean stopCellEditing() {
                InputVerifier verifier = new NameVerifier();
                if (!(verifier.verify(parameterNameTextField))) {
                    parameterNameTextField.setBorder(new LineBorder(Color.red));
                    parameterNameTextField.selectAll();
                    parameterNameTextField.requestFocusInWindow();
                    dialogDescriptor.setValid(false);
                    return false;
                }
                parameterNameTextField.setBorder(originalBorder);
                boolean result = super.stopCellEditing();
                dialogDescriptor.setValid(valid());
                return result;
            }
        });
        nameColumn.setPreferredWidth(200);
        parametersScrollPane.setViewportView(parametersTable);

        org.openide.awt.Mnemonics.setLocalizedText(addParameterButton, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.addParameterButton.text")); // NOI18N
        addParameterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addParameterButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(removeParameterButton, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.removeParameterButton.text")); // NOI18N
        removeParameterButton.setEnabled(false);
        removeParameterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeParameterButtonActionPerformed(evt);
            }
        });

        typeParametersScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.typeParametersScrollPane.border.title"), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION)); // NOI18N

        typeParametersTable.setRowHeight(30);
        typeParametersTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Extends"
            }
        ) {
            Class<?>[] types = new Class<?> [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        TableColumn typeParameterNameTableColumn = typeParametersTable.getColumnModel().getColumn(0);
        JTextField typeParameterNameTextField = new JTextField();
        typeParameterNameTableColumn.setCellEditor (new DefaultCellEditor(typeParameterNameTextField) {
            private final Border originalBorder = typeParameterNameTextField.getBorder();

            @Override
            public boolean stopCellEditing() {
                InputVerifier verifier = new TypeParameterNameVerifier();
                if (!(verifier.verify(typeParameterNameTextField))) {
                    typeParameterNameTextField.setBorder(new LineBorder(Color.red));
                    typeParameterNameTextField.selectAll();
                    typeParameterNameTextField.requestFocusInWindow();
                    super.stopCellEditing();
                    dialogDescriptor.setValid(false);
                    return false;
                }
                typeParameterNameTextField.setBorder(originalBorder);
                boolean result = super.stopCellEditing();
                dialogDescriptor.setValid(valid());
                return result;
            }
        });
        typeParameterNameTableColumn.setPreferredWidth(75);
        TableColumn extendsTableColumn = typeParametersTable.getColumnModel().getColumn(1);
        extendsTableColumn.setCellEditor(new TypeEditor(true, typeParametersTable));
        extendsTableColumn.setCellRenderer(new TypeRenderer());
        extendsTableColumn.setPreferredWidth(350);
        typeParametersScrollPane.setViewportView(typeParametersTable);

        org.openide.awt.Mnemonics.setLocalizedText(addTypeParameterButton, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.addTypeParameterButton.text")); // NOI18N
        addTypeParameterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTypeParameterButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(removeTypeParameterButton, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.removeTypeParameterButton.text")); // NOI18N
        removeTypeParameterButton.setEnabled(false);
        removeTypeParameterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeTypeParameterButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(addThrownTypeButton, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.addThrownTypeButton.text")); // NOI18N
        addThrownTypeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addThrownTypeButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(removeThrownTypeButton, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.removeThrownTypeButton.text")); // NOI18N
        removeThrownTypeButton.setEnabled(false);
        removeThrownTypeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeThrownTypeButtonActionPerformed(evt);
            }
        });

        throwsScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.throwsScrollPane.border.title"), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION)); // NOI18N

        throwsTable.setRowHeight(30);
        throwsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Type"
            }
        ) {
            Class<?>[] types = new Class<?> [] {
                java.lang.String.class
            };

            public Class<?> getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        TableColumn thrownTypeTableColumn = throwsTable.getColumnModel().getColumn(0);
        thrownTypeTableColumn.setCellRenderer(new TypeRenderer());
        thrownTypeTableColumn.setCellEditor(new TypeEditor(false, throwsTable));
        throwsScrollPane.setViewportView(throwsTable);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.jPanel1.border.title"), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION)); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(finalCheckBox, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.finalCheckBox.text")); // NOI18N
        finalCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                finalCheckBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(staticCheckBox, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.staticCheckBox.text")); // NOI18N
        staticCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                staticCheckBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(abstractCheckBox, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.abstractCheckBox.text")); // NOI18N
        abstractCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abstractCheckBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(strictfpCheckBox, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.strictfpCheckBox.text")); // NOI18N
        strictfpCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                strictfpCheckBoxActionPerformed(evt);
            }
        });

        accessComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "package", "private", "protected", "public" }));

        org.openide.awt.Mnemonics.setLocalizedText(nativeCheckBox, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.nativeCheckBox.text")); // NOI18N
        nativeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nativeCheckBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(synchronizedCheckBox, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.synchronizedCheckBox.text")); // NOI18N
        synchronizedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchronizedCheckBoxActionPerformed(evt);
            }
        });

        accessLabel.setLabelFor(accessComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(accessLabel, org.openide.util.NbBundle.getMessage(GenerateMethodPanel.class, "GenerateMethodPanel.accessLabel.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(accessLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(accessComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(staticCheckBox)
                    .addComponent(finalCheckBox)
                    .addComponent(abstractCheckBox)
                    .addComponent(synchronizedCheckBox)
                    .addComponent(nativeCheckBox)
                    .addComponent(strictfpCheckBox))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(accessLabel)
                    .addComponent(accessComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(abstractCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(staticCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(finalCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(synchronizedCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nativeCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(strictfpCheckBox)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameLabel)
                            .addComponent(typeLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(typeTextField)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(browseButton))
                            .addComponent(nameTextField)))
                    .addComponent(parametersScrollPane)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addParameterButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeParameterButton)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(typeParametersScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(addTypeParameterButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removeTypeParameterButton)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(addThrownTypeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removeThrownTypeButton))
                            .addComponent(throwsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addParameterButton, removeParameterButton});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addTypeParameterButton, removeTypeParameterButton});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addThrownTypeButton, removeThrownTypeButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(typeParametersScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE)
                            .addComponent(throwsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(addTypeParameterButton)
                            .addComponent(removeTypeParameterButton)
                            .addComponent(addThrownTypeButton)
                            .addComponent(removeThrownTypeButton)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton)
                    .addComponent(typeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameLabel)
                    .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(parametersScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addParameterButton)
                    .addComponent(removeParameterButton))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        ElementHandle<TypeElement> handle = TypeElementFinder.find(null, null, null);
        if (handle != null) {
            typeTextField.setText(handle.getQualifiedName());
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void addParameterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addParameterButtonActionPerformed
        addRowToParametersTable();
        dialogDescriptor.setValid(false);
        parametersTable.setEnabled(!isParametersTableEmpty());
    }//GEN-LAST:event_addParameterButtonActionPerformed

    private void removeParameterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeParameterButtonActionPerformed
        removeRowFromParametersTable();
        dialogDescriptor.setValid(valid());
        parametersTable.setEnabled(!isParametersTableEmpty());
    }//GEN-LAST:event_removeParameterButtonActionPerformed

    private void addTypeParameterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTypeParameterButtonActionPerformed
        addRowToTypeParametersTable();
        dialogDescriptor.setValid(false);
        typeParametersTable.setEnabled(!isTypeParametersTableEmpty());
    }//GEN-LAST:event_addTypeParameterButtonActionPerformed

    private void removeTypeParameterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeTypeParameterButtonActionPerformed
        removeRowFromTypeParametersTable();
        dialogDescriptor.setValid(valid());
        typeParametersTable.setEnabled(!isTypeParametersTableEmpty());
    }//GEN-LAST:event_removeTypeParameterButtonActionPerformed

    private void addThrownTypeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addThrownTypeButtonActionPerformed
        addRowToThrowsTable();
        dialogDescriptor.setValid(false);
        throwsTable.setEnabled(!isThrowsTableEmpty());
    }//GEN-LAST:event_addThrownTypeButtonActionPerformed

    private void removeThrownTypeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeThrownTypeButtonActionPerformed
        removeRowFromThrowsTable();
        dialogDescriptor.setValid(valid());
        throwsTable.setEnabled(!isThrowsTableEmpty());
    }//GEN-LAST:event_removeThrownTypeButtonActionPerformed

    private void abstractCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_abstractCheckBoxActionPerformed
        if (abstractCheckBox.isSelected()) {
            String access = (String) accessComboBox.getSelectedItem();
            if (access != null && access.equals("private")) { //NOI18N
                accessComboBox.setSelectedItem("package"); //NOI18N
            }
        }
        staticCheckBox.setSelected(false);
        finalCheckBox.setSelected(false);
        synchronizedCheckBox.setSelected(false);
        nativeCheckBox.setSelected(false);
        strictfpCheckBox.setSelected(false);
    }//GEN-LAST:event_abstractCheckBoxActionPerformed

    private void staticCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_staticCheckBoxActionPerformed
        if (staticCheckBox.isSelected()) {
            abstractCheckBox.setSelected(false);
        }
    }//GEN-LAST:event_staticCheckBoxActionPerformed

    private void finalCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_finalCheckBoxActionPerformed
        if (finalCheckBox.isSelected()) {
            abstractCheckBox.setSelected(false);
        }
    }//GEN-LAST:event_finalCheckBoxActionPerformed

    private void synchronizedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_synchronizedCheckBoxActionPerformed
        if (synchronizedCheckBox.isSelected()) {
            abstractCheckBox.setSelected(false);
        }
    }//GEN-LAST:event_synchronizedCheckBoxActionPerformed

    private void nativeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nativeCheckBoxActionPerformed
        if (nativeCheckBox.isSelected()) {
            abstractCheckBox.setSelected(false);
            strictfpCheckBox.setSelected(false);
        }
    }//GEN-LAST:event_nativeCheckBoxActionPerformed

    private void strictfpCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strictfpCheckBoxActionPerformed
        if (strictfpCheckBox.isSelected()) {
            abstractCheckBox.setSelected(false);
            nativeCheckBox.setSelected(false);
        }
    }//GEN-LAST:event_strictfpCheckBoxActionPerformed

    private void addRowToParametersTable() {
        parametersTableModel.addRow(new Object[]{
            false,
            EMPTY_STRING,
            EMPTY_STRING
        });
        int row = parametersTable.convertRowIndexToView(parametersTableModel.getRowCount() - 1);
        parametersTable.getSelectionModel().setSelectionInterval(row, row);
    }

    private void removeRowFromParametersTable() {
        int row = parametersTable.convertRowIndexToModel(parametersTable.getSelectedRow());
        parametersTableModel.removeRow(row);
        if (parametersTableModel.getRowCount() > 0) {
            if (row >= 1) {
                row = parametersTable.convertRowIndexToView(row - 1);
            } else if (row == 0) {
                row = parametersTable.convertRowIndexToView(row);
            }
            parametersTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    private void addRowToTypeParametersTable() {
        typeParametersTableModel.addRow(new Object[]{
            EMPTY_STRING,
            EMPTY_STRING
        });
        int row = parametersTable.convertRowIndexToView(parametersTableModel.getRowCount() - 1);
        parametersTable.getSelectionModel().setSelectionInterval(row, row);
    }

    private void removeRowFromTypeParametersTable() {
        int row = typeParametersTable.convertRowIndexToModel(typeParametersTable.getSelectedRow());
        typeParametersTableModel.removeRow(row);
        if (typeParametersTable.getRowCount() > 0) {
            if (row >= 1) {
                row = typeParametersTable.convertRowIndexToView(row - 1);
            } else if (row == 0) {
                row = typeParametersTable.convertRowIndexToView(row);
            }
            typeParametersTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    private void addRowToThrowsTable() {
        throwsTableModel.addRow(new Object[]{
            EMPTY_STRING
        });
        int row = throwsTable.convertRowIndexToView(throwsTableModel.getRowCount() - 1);
        throwsTable.getSelectionModel().setSelectionInterval(row, row);
    }

    private void removeRowFromThrowsTable() {
        int row = throwsTable.convertRowIndexToModel(throwsTable.getSelectedRow());
        throwsTableModel.removeRow(row);
        if (throwsTable.getRowCount() > 0) {
            if (row >= 1) {
                row = throwsTable.convertRowIndexToView(row - 1);
            } else if (row == 0) {
                row = throwsTable.convertRowIndexToView(row);
            }
            throwsTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    private boolean isParametersTableEmpty() {
        return parametersTable.getRowCount() == 0;
    }

    private boolean isTypeParametersTableEmpty() {
        return typeParametersTable.getRowCount() == 0;
    }

    private boolean isThrowsTableEmpty() {
        return throwsTable.getRowCount() == 0;
    }

    private boolean valid() {
        for (int row = 0; row < parametersTableModel.getRowCount(); row++) {
            String type = (String) parametersTableModel.getValueAt(row, 1);
            if (!type.matches(TYPE_REGEX)) {
                return false;
            }
            String name = (String) parametersTableModel.getValueAt(row, 2);
            if (!name.matches(NAME_REGEX)) {
                return false;
            }
        }
        for (int row = 0; row < typeParametersTableModel.getRowCount(); row++) {
            String name = (String) typeParametersTableModel.getValueAt(row, 0);
            if (!name.matches(TYPE_PARAMETER_NAME_REGEX)) {
                return false;
            }
            String type = (String) typeParametersTableModel.getValueAt(row, 1);
            if (!type.matches(TYPE_REGEX_EMPTY_ALLOWED)) {
                return false;
            }
        }
        for (int row = 0; row < throwsTableModel.getRowCount(); row++) {
            String type = (String) throwsTableModel.getValueAt(row, 0);
            if (!type.matches(TYPE_REGEX)) {
                return false;
            }
        }
        String type = typeTextField.getText();
        if (!type.matches(TYPE_REGEX)) {
            return false;
        }
        String name = nameTextField.getText();
        return name.matches(NAME_REGEX);
    }

    void setDialogDescriptor(DialogDescriptor dialogDescriptor) {
        this.dialogDescriptor = dialogDescriptor;
    }

    String getMethodAccess() {
        return (String) accessComboBox.getSelectedItem();
    }

    boolean isAbstractMethod() {
        return abstractCheckBox.isSelected();
    }

    boolean isStaticMethod() {
        return staticCheckBox.isSelected();
    }

    boolean isFinalMethod() {
        return finalCheckBox.isSelected();
    }

    boolean isSynchronizedMethod() {
        return synchronizedCheckBox.isSelected();
    }

    boolean isNativeMethod() {
        return nativeCheckBox.isSelected();
    }

    boolean isStrictfpMethod() {
        return strictfpCheckBox.isSelected();
    }

    String getMethodType() {
        return typeTextField.getText();
    }

    String getMethodName() {
        return nameTextField.getText();
    }

    List<?> getMethodParameters() {
        return parametersTableModel.getDataVector();
    }

    List<?> getMethodTypeParameters() {
        return typeParametersTableModel.getDataVector();
    }

    List<?> getMethodThrownTypes() {
        return throwsTableModel.getDataVector();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox abstractCheckBox;
    private javax.swing.JComboBox<String> accessComboBox;
    private javax.swing.JLabel accessLabel;
    private javax.swing.JButton addParameterButton;
    private javax.swing.JButton addThrownTypeButton;
    private javax.swing.JButton addTypeParameterButton;
    private javax.swing.JButton browseButton;
    private javax.swing.JCheckBox finalCheckBox;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JCheckBox nativeCheckBox;
    private javax.swing.JScrollPane parametersScrollPane;
    private javax.swing.JTable parametersTable;
    private javax.swing.JButton removeParameterButton;
    private javax.swing.JButton removeThrownTypeButton;
    private javax.swing.JButton removeTypeParameterButton;
    private javax.swing.JCheckBox staticCheckBox;
    private javax.swing.JCheckBox strictfpCheckBox;
    private javax.swing.JCheckBox synchronizedCheckBox;
    private javax.swing.JScrollPane throwsScrollPane;
    private javax.swing.JTable throwsTable;
    private javax.swing.JLabel typeLabel;
    private javax.swing.JScrollPane typeParametersScrollPane;
    private javax.swing.JTable typeParametersTable;
    private javax.swing.JTextField typeTextField;
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
                parameterTypeTextField.setText(handle.getQualifiedName());
            }
        }

        void updateTypeValue(String type) {
            parameterTypeTextField.setText(type);
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
            parameterTypeTextField = new javax.swing.JTextField();
            browseButton = new javax.swing.JButton();
            Mnemonics.setLocalizedText(
                    browseButton, org.openide.util.NbBundle.getMessage(TypePanel.class, "TypePanel.browseButton.text")); // NOI18N
            javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
            this.setLayout(layout);
            layout.setHorizontalGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(parameterTypeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(browseButton)
                                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
            layout.setVerticalGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(parameterTypeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(browseButton))
                                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
        }// </editor-fold>
        // Variables declaration - do not modify
        private javax.swing.JButton browseButton;
        protected javax.swing.JTextField parameterTypeTextField;
        // End of variables declaration
    }

    private class TypeRenderer extends GenerateMethodPanel.TypePanel implements TableCellRenderer {

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

    private class TypeEditor extends GenerateMethodPanel.TypePanel implements TableCellEditor {

        protected transient ChangeEvent changeEvent;
        private final Border originalBorder = parameterTypeTextField.getBorder();
        private final boolean emptyAllowed;
        private final JTable owner;

        public TypeEditor(boolean emptyAllowed, JTable owner) {
            this.emptyAllowed = emptyAllowed;
            this.owner = owner;
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            this.setBackground(table.getSelectionBackground());
            updateTypeValue((String) value);
            return this;
        }

        @Override
        public Object getCellEditorValue() {
            return parameterTypeTextField.getText();
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
            InputVerifier verifier = new TypeVerifier(emptyAllowed);
            if (!(verifier.verify(parameterTypeTextField))) {
                parameterTypeTextField.setBorder(new LineBorder(Color.red));
                parameterTypeTextField.selectAll();
                parameterTypeTextField.requestFocusInWindow();
                fireEditingStopped();
                dialogDescriptor.setValid(false);
                return false;
            }
            parameterTypeTextField.setBorder(originalBorder);
            fireEditingStopped();
            if (owner.equals(parametersTable)) {
                String parameterName = suggestParameterName(parameterTypeTextField.getText());
                int selectedRow = parametersTable.getSelectedRow();
                int parameterNameColumn = 2;
                parametersTableModel.setValueAt(parameterName, selectedRow, parameterNameColumn);
            }
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

        private final boolean emptyAllowed;

        public TypeVerifier(boolean emptyAllowed) {
            this.emptyAllowed = emptyAllowed;
        }

        @Override
        public boolean verify(JComponent input) {
            String text = ((JTextField) input).getText();
            return emptyAllowed ? text.matches(TYPE_REGEX_EMPTY_ALLOWED) : text.matches(TYPE_REGEX);
        }
    }

    private class NameVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
            String text = ((JTextField) input).getText();
            return text.matches(NAME_REGEX);
        }
    }

    private class TypeParameterNameVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent input) {
            String text = ((JTextField) input).getText();
            return text.matches(TYPE_PARAMETER_NAME_REGEX);
        }
    }
}
