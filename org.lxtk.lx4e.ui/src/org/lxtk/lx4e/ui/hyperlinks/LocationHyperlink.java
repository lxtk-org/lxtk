/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.EditorHelper;

/**
 * A hyperlink that opens a given {@link Location}.
 */
public class LocationHyperlink
    extends AbstractHyperlink
{
    private final Location location;

    /**
     * Constructor.
     *
     * @param region the hyperlink region (not <code>null</code>)
     * @param text optional text for this hyperlink (may be <code>null</code>)
     * @param location the target location for this hyperlink
     *  (not <code>null</code>)
     */
    public LocationHyperlink(IRegion region, String text, Location location)
    {
        super(region, text);
        this.location = Objects.requireNonNull(location);
    }

    /**
     * Returns the target location for this hyperlink.
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
        try
        {
            editorHelper.openEditor(page, location, true);
        }
        catch (PartInitException e)
        {
            StatusManager.getManager().handle(e.getStatus(),
                StatusManager.LOG | StatusManager.SHOW);
        }
    }

    /**
     * Returns the {@link EditorHelper} for this hyperlink.
     *
     * @return the editor helper (not <code>null</code>)
     */
    protected EditorHelper getEditorHelper()
    {
        return DefaultEditorHelper.INSTANCE;
    }

    private static IWorkbenchPage getWorkbenchPage()
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            return null;
        return window.getActivePage();
    }
}
