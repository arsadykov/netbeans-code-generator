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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.prefs.Preferences;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 *
 * @author Arthur Sadykov
 */
public class GenerateGetterInvocationsDialog {

    private static final String X = "GenerateGetterInvocationsDialogX"; //NOI18N
    private static final String Y = "GenerateGetterInvocationsDialogY"; //NOI18N
    private static final String WIDTH = "GenerateGetterInvocationsDialogWidth"; //NOI18N
    private static final String HEIGHT = "GenerateGetterInvocationsDialogHeight"; //NOI18N
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;
    private final Dialog dialog;
    private final DialogDescriptor dialogDescriptor;

    private GenerateGetterInvocationsDialog(LocalMembersAndVarsPanel localMembersAndVarsPanel) {
        dialogDescriptor = new DialogDescriptor(
                localMembersAndVarsPanel,
                NbBundle.getMessage(GenerateGetterInvocationsDialog.class, "DN_Generate_Getter_Invocations")); //NOI18N
        localMembersAndVarsPanel.setDialogDescriptor(dialogDescriptor);
        dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialogDescriptor.setValid(false);
        dialog.setBounds(getBounds());
        dialog.setVisible(true);
    }

    public static GenerateGetterInvocationsDialog createAndShow(LocalMembersAndVarsPanel localMembersAndVarsPanel) {
        return new GenerateGetterInvocationsDialog(localMembersAndVarsPanel);
    }

    public boolean isOkButtonPushed() {
        return dialogDescriptor.getValue() == DialogDescriptor.OK_OPTION;
    }

    public void dispose() {
        dialog.dispose();
    }

    private Rectangle getBounds() {
        Preferences preferences = NbPreferences.forModule(GenerateGetterInvocationsDialog.class);
        Frame mainWindow = WindowManager.getDefault().getMainWindow();
        int windowWidth = mainWindow.getWidth();
        int windowHeight = mainWindow.getHeight();
        int dialogWidth = preferences.getInt(WIDTH, DEFAULT_WIDTH);
        int dialogHeight = preferences.getInt(HEIGHT, DEFAULT_HEIGHT);
        int dialogX = preferences.getInt(X, windowWidth / 2 - dialogWidth / 2);
        int dialogY = preferences.getInt(Y, windowHeight / 2 - dialogHeight / 2);
        return new Rectangle(dialogX, dialogY, dialogWidth, dialogHeight);
    }

    public void saveBounds() {
        Preferences preferences = NbPreferences.forModule(GenerateGetterInvocationsDialog.class);
        if (isClipped()) {
            Frame mainWindow = WindowManager.getDefault().getMainWindow();
            int windowWidth = mainWindow.getWidth();
            int windowHeight = mainWindow.getHeight();
            preferences.putInt(X, windowWidth / 2 - dialog.getWidth() / 2);
            preferences.putInt(Y, windowHeight / 2 - dialog.getHeight() / 2);
            preferences.putInt(WIDTH, dialog.getWidth());
            preferences.putInt(HEIGHT, dialog.getHeight());
        } else {
            preferences.putInt(X, dialog.getX());
            preferences.putInt(Y, dialog.getY());
            preferences.putInt(WIDTH, dialog.getWidth());
            preferences.putInt(HEIGHT, dialog.getHeight());
        }
    }

    private boolean isClipped() {
        int dialogArea = dialog.getWidth() * dialog.getHeight();
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = environment.getScreenDevices();
        Rectangle bounds;
        int boundsArea = 0;
        for (GraphicsDevice device : devices) {
            bounds = device.getDefaultConfiguration().getBounds();
            if (bounds.intersects(dialog.getBounds())) {
                bounds = bounds.intersection(dialog.getBounds());
                boundsArea += (bounds.width * bounds.height);
            }
        }
        return boundsArea != dialogArea;
    }
}
