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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.ui.TypeElementFinder;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.netbeans.spi.editor.codegen.CodeGeneratorContextProvider;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public class ImportGenerator implements CodeGenerator {

    private final JTextComponent component;
    private final Document document;

    /**
     *
     * @param context containing JTextComponent and possibly other items registered by
     *                {@link CodeGeneratorContextProvider}
     */
    private ImportGenerator(Lookup context) { // Good practice is not to save Lookup outside ctor
        component = context.lookup(JTextComponent.class);
        document = component.getDocument();
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(ImportGenerator.class, "DN_Import"); //NOI18N
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code dialog
     */
    @Override
    public void invoke() {
        ElementHandle<TypeElement> typeHandle = TypeElementFinder.find(null, null, null);
        if (typeHandle == null) {
            return;
        }
        String qualifiedName = typeHandle.getQualifiedName();
        if (qualifiedName.startsWith("java.lang")) { //NOI18N
            return;
        }
        JavaSource javaSource = JavaSource.forDocument(document);
        if (javaSource == null) {
            throw new IllegalStateException("The document is not associated with data type providing the JavaSource."); //NOI18N
        }
        try {
            javaSource.runModificationTask(copy -> {
                JavaSource.Phase phase = copy.toPhase(JavaSource.Phase.PARSED);
                if (phase.compareTo(JavaSource.Phase.PARSED) < 0) {
                    throw new IllegalStateException("Cannot move state to Phase.PARSED."); //NOI18N
                }
                CompilationUnitTree compilationUnitTree = copy.getCompilationUnit();
                List<? extends ImportTree> imports = compilationUnitTree.getImports();
                for (ImportTree importTree : imports) {
                    String importIdentifier = importTree.getQualifiedIdentifier().toString();
                    if (importIdentifier.equals(qualifiedName)) {
                        return;
                    }
                }
                TreeMaker treeMaker = copy.getTreeMaker();
                ImportTree importTree = treeMaker.Import(treeMaker.Identifier(qualifiedName), false);
                CompilationUnitTree newCompilationUnitTree;
                int insertIndex = findInsertIndexInImportTree(copy);
                if (insertIndex == -1) {
                    newCompilationUnitTree = treeMaker.addCompUnitImport(compilationUnitTree, importTree);
                } else {
                    newCompilationUnitTree =
                            treeMaker.insertCompUnitImport(compilationUnitTree, insertIndex, importTree);
                }
                copy.rewrite(compilationUnitTree, newCompilationUnitTree);
            }).commit();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private int findInsertIndexInImportTree(CompilationController controller) {
        Trees trees = controller.getTrees();
        CompilationUnitTree compilationUnit = controller.getCompilationUnit();
        List<? extends ImportTree> imports = compilationUnit.getImports();
        SourcePositions sourcePositions = trees.getSourcePositions();
        int size = imports.size();
        switch (size) {
            case 0: {
                return 0;
            }
            case 1: {
                ImportTree currentImport = imports.get(0);
                long currentStartPosition = sourcePositions.getStartPosition(compilationUnit, currentImport);
                if (component.getCaretPosition() < currentStartPosition) {
                    return 0;
                } else {
                    return 1;
                }
            }
            case 2: {
                ImportTree previousImport = imports.get(0);
                long previousStartPosition =
                        sourcePositions.getStartPosition(compilationUnit, previousImport);
                ImportTree currentImport = imports.get(1);
                long currentStartPosition = sourcePositions.getStartPosition(compilationUnit, currentImport);
                if (component.getCaretPosition() < previousStartPosition) {
                    return 0;
                } else if (currentStartPosition < component.getCaretPosition()) {
                    return size;
                } else {
                    return 1;
                }
            }
            default: {
                for (int i = 1; i < size; i++) {
                    ImportTree previousImport = imports.get(i - 1);
                    long previousStartPosition =
                            sourcePositions.getStartPosition(compilationUnit, previousImport);
                    ImportTree currentImport = imports.get(i);
                    long currentStartPosition =
                            sourcePositions.getStartPosition(compilationUnit, currentImport);
                    if (i < size - 1) {
                        if (component.getCaretPosition() < previousStartPosition) {
                            return i - 1;
                        } else if (previousStartPosition < component.getCaretPosition()
                                && component.getCaretPosition() < currentStartPosition) {
                            return i;
                        }
                    } else {
                        if (component.getCaretPosition() < currentStartPosition) {
                            return size - 1;
                        }
                        return size;
                    }
                }
            }
        }
        return -1;
    }

    @MimeRegistration(mimeType = "text/x-java", service = CodeGenerator.Factory.class, position = 11000)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            return Collections.singletonList(new ImportGenerator(context));
        }
    }
}
