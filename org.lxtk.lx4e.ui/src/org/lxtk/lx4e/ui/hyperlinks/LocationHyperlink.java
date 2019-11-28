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
package org.lxtk.lx4e.ui.hyperlinks;

import java.util.Objects;

import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.Location;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.lxtk.DocumentUri;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.EditorHelper;

/**
 * TODO JavaDoc
 */
public class LocationHyperlink
    extends AbstractHyperlink
{
    private final Location location;

    /**
     * TODO JavaDoc
     *
     * @param region not <code>null</code>
     * @param text may be <code>null</code>
     * @param location not <code>null</code>
     */
    public LocationHyperlink(IRegion region, String text, Location location)
    {
        super(region, text);
        this.location = Objects.requireNonNull(location);
    }

    /**
     * TODO JavaDoc
     *
     * @return the target location (never <code>null</code>)
     */
    public final Location getLocation()
    {
        return location;
    }

    @Override
    public void open()
    {
        IWorkbenchPage page = getWorkbenchPage();
        if (page == null)
            return;
        EditorHelper editorHelper = getEditorHelper();
        IEditorPart editor = null;
        try
        {
            editor = editorHelper.openEditor(page, DocumentUri.convert(
                location.getUri()), true);
        }
        catch (PartInitException e)
        {
            Activator.logError(e);
        }
        if (editor != null)
            editorHelper.selectTextRange(editor, location.getRange());
    }

    /**
     * TODO JavaDoc
     *
     * @return an editor helper (not <code>null</code>)
     */
    protected EditorHelper getEditorHelper()
    {
        return DefaultEditorHelper.INSTANCE;
    }

    private static IWorkbenchPage getWorkbenchPage()
    {
        IWorkbenchWindow window =
            PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            return null;
        return window.getActivePage();
    }
}
