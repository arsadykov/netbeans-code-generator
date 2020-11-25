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

import com.github.isarthur.netbeans.editor.generator.java.ui.GenerateMethodDialog;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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

public class MethodGenerator implements CodeGenerator {

    private final JTextComponent editor;
    private GenerateMethodDialog dialog;
    private TreeUtilities treeUtilities;
    private Trees trees;
    private int caretPosition;
    private JavaSource javaSource;
    private TreePath currentPath;
    private CompilationUnitTree compilationUnit;
    private int insertIndex;
    private boolean isClass;

    /**
     *
     * @param context containing JTextComponent and possibly other items registered by
     *                {@link CodeGeneratorContextProvider}
     */
    private MethodGenerator(Lookup context) { // Good practice is not to save Lookup outside ctor
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
        return NbBundle.getMessage(FieldGenerator.class, "DN_Method"); //NOI18N
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code
     * dialog
     */
    @Override
    public void invoke() {
        try {
            javaSource.runModificationTask(workingCopy -> {
                workingCopy.toPhase(JavaSource.Phase.RESOLVED);
                compilationUnit = workingCopy.getCompilationUnit();
                trees = workingCopy.getTrees();
                treeUtilities = workingCopy.getTreeUtilities();
                currentPath = treeUtilities.pathFor(caretPosition);
                TreePath classOrInterfacePath = treeUtilities.getPathElementOfKind(
                        EnumSet.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE), currentPath);
                if (classOrInterfacePath != null) {
                    Tree classOrInterfaceTree = classOrInterfacePath.getLeaf();
                    ClassTree oldTree = (ClassTree) classOrInterfaceTree;
                    setInsertIndex(oldTree);
                    if (classOrInterfaceTree.getKind() == Tree.Kind.CLASS) {
                        isClass = true;
                        dialog = GenerateMethodDialog.createAndShow(isClass);
                    } else {
                        isClass = false;
                        dialog = GenerateMethodDialog.createAndShow(isClass);
                    }
                    if (dialog.isOkButtonPushed()) {
                        insertMethodIntoClassOrInterface(workingCopy);
                    }
                }
            }).commit();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        dialog.saveBounds();
        dialog.dispose();
    }

    private void insertMethodIntoClassOrInterface(WorkingCopy workingCopy) {
        ClassTree oldClassTree = getClassOrInterfaceTree(workingCopy);
        ClassTree newClassTree = oldClassTree;
        TreeMaker make = workingCopy.getTreeMaker();
        Set<Modifier> modifiers = getMethodModifiers();
        String methodType = dialog.getMethodType();
        String methodName = dialog.getMethodName();
        List<?> parameters = dialog.getMethodParameters();
        List<VariableTree> methodParameters = getMethodParameters(parameters, make);
        List<?> typeParameters = dialog.getMethodTypeParameters();
        List<TypeParameterTree> methodTypeParameters = getMethodTypeParameters(typeParameters, make);
        List<?> thrownTypes = dialog.getMethodThrownTypes();
        List<ExpressionTree> methodThrownTypes = getMethodThrownTypes(thrownTypes, make);
        BlockTree block = make.Block(Collections.emptyList(), false);
        newClassTree = make.insertClassMember(newClassTree,
                insertIndex,
                make.Method(make.Modifiers(modifiers),
                        methodName,
                        make.Type(methodType),
                        methodTypeParameters,
                        methodParameters,
                        methodThrownTypes,
                        (isAbstractMethod() || isNativeMethod() || !isClass) ? null : block,
                        null)
        );
        workingCopy.rewrite(oldClassTree, newClassTree);
    }

    private ClassTree getClassOrInterfaceTree(WorkingCopy workingCopy) {
        TreePath classOrInterfacePath = workingCopy
                .getTreeUtilities()
                .getPathElementOfKind(Set.of(Tree.Kind.INTERFACE, Tree.Kind.CLASS), currentPath);
        if (classOrInterfacePath == null) {
            throw new IllegalStateException("No class or interface in the java file!"); //NOI18N
        }
        return (ClassTree) classOrInterfacePath.getLeaf();
    }

    private List<VariableTree> getMethodParameters(List<?> parameters, TreeMaker make) {
        int numberOfRows = parameters.size();
        List<VariableTree> methodParameters = new ArrayList<>();
        for (int row = 0; row < numberOfRows; row++) {
            Set<Modifier> parameterModifiers = new HashSet<>();
            boolean finalParameter = (boolean) ((List) parameters.get(row)).get(0);
            if (finalParameter) {
                parameterModifiers.add(Modifier.FINAL);
            }
            String parameterType = (String) ((List) parameters.get(row)).get(1);
            String parameterName = (String) ((List) parameters.get(row)).get(2);
            VariableTree parameter =
                    make.Variable(
                            make.Modifiers(parameterModifiers),
                            parameterName,
                            make.Type(parameterType),
                            null);
            methodParameters.add(parameter);
        }
        return Collections.unmodifiableList(methodParameters);
    }

    private List<TypeParameterTree> getMethodTypeParameters(List<?> typeParameters, TreeMaker make) {
        int numberOfRows = typeParameters.size();
        List<TypeParameterTree> methodTypeParameters = new ArrayList<>();
        for (int row = 0; row < numberOfRows; row++) {
            String typeParameterName = (String) ((List) typeParameters.get(row)).get(0);
            String typeParameterBound = (String) ((List) typeParameters.get(row)).get(1);
            List<ExpressionTree> bounds = new ArrayList<>();
            if (!typeParameterBound.isEmpty()) {
                bounds.add(make.QualIdent(typeParameterBound));
            }
            TypeParameterTree typeParameterTree =
                    make.TypeParameter(
                            typeParameterName,
                            bounds);
            methodTypeParameters.add(typeParameterTree);
        }
        return Collections.unmodifiableList(methodTypeParameters);
    }

    private List<ExpressionTree> getMethodThrownTypes(List<?> thrownTypes, TreeMaker make) {
        int numberOfRows = thrownTypes.size();
        List<ExpressionTree> methodThrownTypes = new ArrayList<>();
        for (int row = 0; row < numberOfRows; row++) {
            String thrownType = (String) ((List) thrownTypes.get(row)).get(0);
            methodThrownTypes.add(make.QualIdent(thrownType));
        }
        return Collections.unmodifiableList(methodThrownTypes);
    }

    private Set<Modifier> getMethodModifiers() {
        Set<Modifier> modifiers = new HashSet<>();
        String methodAccess = dialog.getMethodAccess();
        boolean abstractMethod = dialog.isAbstractMethod();
        boolean staticMethod = dialog.isStaticMethod();
        boolean finalMethod = dialog.isFinalMethod();
        boolean synchronizedMethod = dialog.isSynchronizedMethod();
        boolean nativeMethod = dialog.isNativeMethod();
        boolean strictfpMethod = dialog.isStrictfpMethod();
        switch (methodAccess) {
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
        if (abstractMethod) {
            modifiers.add(Modifier.ABSTRACT);
        }
        if (staticMethod) {
            modifiers.add(Modifier.STATIC);
        }
        if (finalMethod) {
            modifiers.add(Modifier.FINAL);
        }
        if (synchronizedMethod) {
            modifiers.add(Modifier.SYNCHRONIZED);
        }
        if (nativeMethod) {
            modifiers.add(Modifier.NATIVE);
        }
        if (strictfpMethod) {
            modifiers.add(Modifier.STRICTFP);
        }
        return Collections.unmodifiableSet(modifiers);
    }

    private boolean isAbstractMethod() {
        return dialog.isAbstractMethod();
    }

    private boolean isNativeMethod() {
        return dialog.isNativeMethod();
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
                    if (i < size - 1) {
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

    @MimeRegistration(mimeType = "text/x-java", service = CodeGenerator.Factory.class, position = 7000) //NOI18N
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
                    TreePath blockPath =
                            treeUtilities.getPathElementOfKind(Tree.Kind.BLOCK, currentPath);
                    if (blockPath == null) {
                        insideBlock.set(false);
                    }
                }, true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            return !insideBlock.get()
                    ? Collections.singletonList(new MethodGenerator(context))
                    : Collections.emptyList();
        }
    }
}
