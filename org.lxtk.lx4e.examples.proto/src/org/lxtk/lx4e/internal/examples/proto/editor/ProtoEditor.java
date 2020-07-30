/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vladimir Piskarev (1C) - initial API and implementation
 *******************************************************************************/
package org.lxtk.lx4e.internal.examples.proto.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.lxtk.lx4e.internal.examples.proto.Activator;
import org.lxtk.lx4e.internal.examples.proto.ProtoSourceViewerConfiguration;

/**
 * Proto text editor.
 */
public class ProtoEditor
    extends AbstractDecoratedTextEditor
{
    @Override
    protected void initializeEditor()
    {
        IPreferenceStore preferenceStore = new ChainedPreferenceStore(new IPreferenceStore[] {
            Activator.getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() });
        setPreferenceStore(preferenceStore);
        setDocumentProvider(Activator.getDefault().getDocumentProvider());
        setSourceViewerConfiguration(new ProtoSourceViewerConfiguration(preferenceStore, this));
        setEditorContextMenuId("#ProtoEditorContext"); //$NON-NLS-1$
        setRulerContextMenuId("#ProtoRulerContext"); //$NON-NLS-1$
    }

    @Override
    protected void initializeKeyBindingScopes()
    {
        setKeyBindingScopes(new String[] { "org.lxtk.lx4e.examples.proto.editor.scope" }); //$NON-NLS-1$
    }
}
