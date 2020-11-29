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
package com.github.isarthur.netbeans.editor.generator.java;

import com.github.isarthur.netbeans.editor.generator.java.ui.GenerateFieldsDialog;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Modifier;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.netbeans.spi.editor.codegen.CodeGeneratorContextProvider;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Arthur Sadykov
 */
public class FieldGenerator implements CodeGenerator {

    private final JTextComponent editor;
    private GenerateFieldsDialog dialog;
    private Trees trees;
    private TreeUtilities treeUtilities;
    private TreePath currentPath;
    private CompilationUnitTree compilationUnit;
    private int caretPosition;
    private JavaSource javaSource;
    private int insertIndex;

    /**
     *
     * @param context containing JTextComponent and possibly other items registered by
     *                {@link CodeGeneratorContextProvider}
     */
    private FieldGenerator(Lookup context) { // Good practice is not to save Lookup outside ctor
        editor = context.lookup(JTextComponent.class);
        initialize();
    }

    private void initialize() {
        caretPosition = editor.getCaretPosition();
        Document document = editor.getDocument();
        javaSource = JavaSource.forDocument(document);
        if (javaSource == null) {
            throw new IllegalStateException("The document is not associated with data type providing the JavaSource."); //NOI18N
        }
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(FieldGenerator.class, "DN_Fields"); //NOI18N
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code dialog
     */
    @Override
    public void invoke() {
        dialog = GenerateFieldsDialog.createAndShow();
        if (dialog.isOkButtonPushed()) {
            try {
                javaSource.runModificationTask(workingCopy -> {
                    workingCopy.toPhase(JavaSource.Phase.RESOLVED);
                    compilationUnit = workingCopy.getCompilationUnit();
                    trees = workingCopy.getTrees();
                    treeUtilities = workingCopy.getTreeUtilities();
                    currentPath = treeUtilities.pathFor(caretPosition);
                    TreePath classOrInterfacePath =
                            treeUtilities.getPathElementOfKind(Set.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE), currentPath);
                    if (classOrInterfacePath == null) {
                        return;
                    }
                    ClassTree oldTree = (ClassTree) classOrInterfacePath.getLeaf();
                    setInsertIndex(oldTree);
                    insertFieldsIntoClass(workingCopy);
                }).commit();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        dialog.saveBounds();
        dialog.dispose();
    }

    private void insertFieldsIntoClass(WorkingCopy workingCopy) {
        ClassTree oldClassOrInterfaceTree = getClassOrInterfaceTree(workingCopy);
        ClassTree newClassOrInterfaceTree = oldClassOrInterfaceTree;
        TreeMaker make = workingCopy.getTreeMaker();
        List<?> data = dialog.getData();
        int numberOfRows = data.size();
        for (int row = 0; row < numberOfRows; row++) {
            Set<Modifier> modifiers = new HashSet<>();
            String fieldAccess = (String) ((List) data.get(row)).get(0);
            boolean staticField = (boolean) ((List) data.get(row)).get(1);
            boolean finalField = (boolean) ((List) data.get(row)).get(2);
            boolean transientField = (boolean) ((List) data.get(row)).get(3);
            boolean volatileField = (boolean) ((List) data.get(row)).get(4);
            switch (fieldAccess) {
                case "public": { //NOI18N
                    modifiers.add(Modifier.PUBLIC);
                    break;
                }
                case "protected": { //NOI18N
                    modifiers.add(Modifier.PROTECTED);
                    break;
                }
                case "private": { //NOI18N
                    modifiers.add(Modifier.PRIVATE);
                    break;
                }
            }
            if (staticField) {
                modifiers.add(Modifier.STATIC);
            }
            if (finalField) {
                modifiers.add(Modifier.FINAL);
            }
            if (transientField) {
                modifiers.add(Modifier.TRANSIENT);
            }
            if (volatileField) {
                modifiers.add(Modifier.VOLATILE);
            }
            String fieldType = (String) ((List) data.get(row)).get(5);
            String fieldName = (String) ((List) data.get(row)).get(6);
            String fieldValue = (String) ((List) data.get(row)).get(7);
            newClassOrInterfaceTree = make.insertClassMember(
                    newClassOrInterfaceTree,
                    insertIndex + row,
                    make.Variable(
                            make.Modifiers(modifiers),
                            fieldName,
                            make.QualIdent(fieldType),
                            fieldValue.isEmpty() ? null : make.Identifier(fieldValue)));
        }
        workingCopy.rewrite(oldClassOrInterfaceTree, newClassOrInterfaceTree);
    }

    private ClassTree getClassOrInterfaceTree(WorkingCopy workingCopy) {
        TreePath classOrInterfacePath = workingCopy.getTreeUtilities()
                .getPathElementOfKind(Set.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE), currentPath);
        if (classOrInterfacePath == null) {
            throw new IllegalStateException("No class or interface in the java file!"); //NOI18N
        }
        return (ClassTree) classOrInterfacePath.getLeaf();
    }

    private void setInsertIndex(ClassTree classTree) {
        List<? extends Tree> members = classTree.getMembers();
        SourcePositions sourcePositions = trees.getSourcePositions();
        int size = members.size();
        switch (size) {
            case 1: {
                Tree member = members.get(0);
                long currentStartPosition = sourcePositions.getStartPosition(compilationUnit, member);
                if (caretPosition < currentStartPosition) {
                    insertIndex = 0;
                } else {
                    insertIndex = 1;
                }
                break;
            }
            case 2: {
                Tree previousMember = members.get(0);
                long previousStartPosition = sourcePositions.getStartPosition(compilationUnit, previousMember);
                Tree currentMember = members.get(1);
                long currentStartPosition = sourcePositions.getStartPosition(compilationUnit, currentMember);
                if (caretPosition < previousStartPosition) {
                    insertIndex = 0;
                } else if (currentStartPosition < caretPosition) {
                    insertIndex = size;
                } else {
                    insertIndex = 1;
                }
                break;
            }
            default:
                for (int i = 1; i < size; i++) {
                    Tree previousMember = members.get(i - 1);
                    long previousStartPosition = sourcePositions.getStartPosition(compilationUnit, previousMember);
                    Tree currentMember = members.get(i);
                    long currentStartPosition = sourcePositions.getStartPosition(compilationUnit, currentMember);
                    if (i == 1 && caretPosition < previousStartPosition) {
                        insertIndex = i - 1;
                        break;
                    } else if (i < size - 1) {
                        if (previousStartPosition < caretPosition && caretPosition < currentStartPosition) {
                            insertIndex = i;
                            break;
                        }
                    } else {
                        if (currentStartPosition < caretPosition) {
                            insertIndex = size;
                        } else {
                            insertIndex = i;
                        }
                    }
                }
                break;
        }
    }

    @MimeRegistration(mimeType = "text/x-java", service = CodeGenerator.Factory.class, position = 6000) //NOI18N
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            JTextComponent editor = context.lookup(JTextComponent.class);
            JavaSource javaSource = JavaSource.forDocument(editor.getDocument());
            if (javaSource == null) {
                throw new IllegalStateException("The document is not associated with data type providing the JavaSource."); //NOI18N
            }
            AtomicBoolean insideBlock = new AtomicBoolean(true);
            try {
                javaSource.runUserActionTask(controller -> {
                    JavaSource.Phase phase = controller.toPhase(JavaSource.Phase.RESOLVED);
                    if (phase.compareTo(JavaSource.Phase.RESOLVED) < 0) {
                        return;
                    }
                    TreeUtilities treeUtilities = controller.getTreeUtilities();
                    TreePath currentPath = treeUtilities.pathFor(editor.getCaretPosition());
                    TreePath blockPath = treeUtilities.getPathElementOfKind(Tree.Kind.BLOCK, currentPath);
                    if (blockPath == null) {
                        insideBlock.set(false);
                    }
                }, true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            return !insideBlock.get()
                    ? Collections.singletonList(new FieldGenerator(context))
                    : Collections.emptyList();
        }
    }
}
