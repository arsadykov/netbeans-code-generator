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

import java.io.File;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import javax.swing.JEditorPane;
import javax.swing.text.StyledDocument;
import junit.framework.Test;
import static junit.framework.TestCase.assertNotNull;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.netbeans.junit.NbModuleSuite;

/**
 *
 * @author Arthur Sadykov
 */
public class GetterInvocationGeneratorTest extends NbTestCase {

    private final String content =
            "public class X {\n"
            + "\n"
            + "    private int x;\n"
            + "    private int y;\n"
            + "\n"
            + "    public void foo() {\n"
            + "        String string = \"string\";\n"
            + "    }\n"
            + "}";
    private StyledDocument document;
    private JEditorPane editorPane;
    private GetterInvocationGenerator generator;

    public GetterInvocationGeneratorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return NbModuleSuite.createConfiguration(GetterInvocationGeneratorTest.class)
                .clusters("ide")
                .clusters("java")
                .clusters("extide")
                .gui(false)
                .suite();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        clearWorkDir();
        File root = getWorkDir();
        FileObject fo = FileUtil.toFileObject(root);
        FileObject java = FileUtil.createData(fo, "X.java");
        FileLock lock = java.lock();
        try (OutputStream out = java.getOutputStream(lock)) {
            out.write(content.getBytes());
        } finally {
            lock.releaseLock();
        }
        JavaSource javaSource = JavaSource.forFileObject(java);
        assertNotNull("javaSource was null", javaSource);
        DataObject dataObject = DataObject.find(java);
        EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
        document = editorCookie.openDocument();
        editorPane = new JEditorPane();
        editorPane.setDocument(document);
        editorPane.getDocument().putProperty(JavaSource.class, new WeakReference<>(javaSource));
        generator = GetterInvocationGenerator.create(editorPane);
    }

    public void testWhenInvokingOnSelectedVariableOrFieldShouldGenerateGetters() throws Exception {
        String text = document.getText(0, document.getLength());
        String string = "string";
        int indexOfString = text.indexOf(string);
        editorPane.setSelectionStart(indexOfString);
        editorPane.setSelectionEnd(indexOfString + string.length());
        assertEquals("", string, editorPane.getSelectedText());
        generator.invoke();
        String actualText = generator.getText();
        String expectedText =
                "public class X {\n"
                + "\n"
                + "    private int x;\n"
                + "    private int y;\n"
                + "\n"
                + "    public void foo() {\n"
                + "        String string = \"string\";\n"
                + "        boolean empty = string.isEmpty();\n"
                + "        byte[] bytes = string.getBytes(null);\n"
                + "        byte[] bytes1 = string.getBytes(null);\n"
                + "        byte[] bytes2 = string.getBytes();\n"
                + "    }\n"
                + "}";
        assertEquals("Expected source text and actual source text are not equal", expectedText, actualText);
    }

    public void testWhenInvokingOnAnythingThatIsNotVariableOrFieldThenDoNotGenerateAnyCode() throws Exception {
        String text = document.getText(0, document.getLength());
        String string = "private";
        int indexOfString = text.indexOf(string);
        editorPane.setSelectionStart(indexOfString);
        editorPane.setSelectionEnd(indexOfString + string.length());
        assertEquals("", string, editorPane.getSelectedText());
        generator.invoke();
        String actualText = generator.getText();
        assertEquals("Expected source text and actual source text are not equal", content, actualText);
        string = "int";
        indexOfString = text.indexOf(string);
        editorPane.setSelectionStart(indexOfString);
        editorPane.setSelectionEnd(indexOfString + string.length());
        assertEquals("", string, editorPane.getSelectedText());
        generator.invoke();
        actualText = generator.getText();
        assertEquals("Expected source text and actual source text are not equal", content, actualText);
        string = "void";
        indexOfString = text.indexOf(string);
        editorPane.setSelectionStart(indexOfString);
        editorPane.setSelectionEnd(indexOfString + string.length());
        assertEquals("", string, editorPane.getSelectedText());
        generator.invoke();
        actualText = generator.getText();
        assertEquals("Expected source text and actual source text are not equal", content, actualText);
    }

    public void testWhenTextIsNotSelectedThenDoNotGenerateAnyCode() throws Exception {
        editorPane.setCaretPosition(0);
        generator.invoke();
        String actualText = generator.getText();
        assertEquals("Text of document should not change", content, actualText);
    }
}
