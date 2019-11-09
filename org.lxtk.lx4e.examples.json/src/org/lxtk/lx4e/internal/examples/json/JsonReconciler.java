/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
package org.lxtk.lx4e.internal.examples.json;

import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.ui.IWorkingCopyManager;
import org.eclipse.handly.ui.text.reconciler.EditorWorkingCopyReconciler;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.lxtk.DocumentSymbolProvider;
import org.lxtk.lx4e.examples.json.JsonCore;
import org.lxtk.util.Registry;
import org.lxtk.util.SafeRun;

/**
 * TODO JavaDoc
 */
public class JsonReconciler
    extends EditorWorkingCopyReconciler
{
    private Runnable uninstallRunnable;

    /**
     * TODO JavaDoc
     *
     * @param editor not <code>null</code>
     * @param workingCopyManager not <code>null</code>
     */
    public JsonReconciler(IEditorPart editor,
        IWorkingCopyManager workingCopyManager)
    {
        super(editor, workingCopyManager);
    }

    @Override
    public void install(ITextViewer textViewer)
    {
        super.install(textViewer);

        SafeRun.run(rollback ->
        {
            Registry<DocumentSymbolProvider> providers =
                JsonCore.LANG_SERVICE.getDocumentSymbolProviders();
            rollback.add(providers.onDidAdd().subscribe(
                provider -> forceReconciling())::dispose);
            rollback.add(providers.onDidRemove().subscribe(
                provider -> forceReconciling())::dispose);

            rollback.setLogger(e -> Activator.logError(e));
            uninstallRunnable = rollback;
        });
    }

    @Override
    public void uninstall()
    {
        try
        {
            if (uninstallRunnable != null)
                uninstallRunnable.run();
        }
        finally
        {
            super.uninstall();
        }
    }

    @Override
    protected void addElementChangeListener(IElementChangeListener listener)
    {
        JsonCore.addElementChangeListener(listener);
    }

    @Override
    protected void removeElementChangeListener(IElementChangeListener listener)
    {
        JsonCore.removeElementChangeListener(listener);
    }
}
