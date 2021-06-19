/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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
package org.lxtk.lx4e.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.lsp4j.Location;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;
import org.lxtk.lx4e.internal.ui.Activator;

/**
 * Partial implementation of a handler that shows a dialog with a list of items to the user
 * and opens the selected item(s) in the corresponding editor(s).
 */
public abstract class AbstractItemsSelectionHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchSite site = HandlerUtil.getActiveSite(event);
        if (site == null)
            return null;

        SelectionDialog dialog = createSelectionDialog(site.getShell(), event);
        if (dialog == null)
            return null;

        dialog.open();

        Object[] result = dialog.getResult();
        if (result == null || result.length == 0)
            return null;

        IWorkbenchPage page = site.getPage();
        EditorHelper editorHelper = getEditorHelper();
        MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, 0,
            Messages.AbstractItemsSelectionHandler_errorMessage, null);
        for (Object item : result)
        {
            Location location = getLocation(item);
            if (location != null)
            {
                try
                {
                    editorHelper.openEditor(page, location, true);
                }
                catch (PartInitException e)
                {
                    status.merge(e.getStatus());
                }
            }
        }
        if (!status.isOK())
            StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
        return null;
    }

    /**
     * Creates and returns a selection dialog.
     *
     * @param shell a parent shell
     * @param event a handler event (never <code>null</code>)
     * @return the created dialog, or <code>null</code> if none
     */
    protected abstract SelectionDialog createSelectionDialog(Shell shell, ExecutionEvent event);

    /**
     * Returns the location for a selected item.
     *
     * @param item never <code>null</code>
     * @return the corresponding location, or <code>null</code> if none
     */
    protected abstract Location getLocation(Object item);

    /**
     * Returns the {@link EditorHelper} for this handler.
     *
     * @return the editor helper (not <code>null</code>)
     */
    protected EditorHelper getEditorHelper()
    {
        return DefaultEditorHelper.INSTANCE;
    }
}
