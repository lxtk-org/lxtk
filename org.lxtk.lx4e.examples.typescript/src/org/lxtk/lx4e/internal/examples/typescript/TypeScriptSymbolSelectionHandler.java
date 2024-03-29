/*******************************************************************************
 * Copyright (c) 2020, 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.internal.examples.typescript;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.WorkspaceSymbolLocation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.lxtk.WorkspaceSymbolProvider;
import org.lxtk.lx4e.examples.typescript.TypeScriptCore;
import org.lxtk.lx4e.ui.AbstractItemsSelectionHandler;
import org.lxtk.lx4e.ui.symbols.WorkspaceSymbolItem;
import org.lxtk.lx4e.ui.symbols.WorkspaceSymbolSelectionDialog;

/**
 * A handler that shows a dialog with a list of symbols to the user
 * and opens the selected symbol(s) in the corresponding editor(s).
 */
public class TypeScriptSymbolSelectionHandler
    extends AbstractItemsSelectionHandler
{
    @Override
    protected SelectionDialog createSelectionDialog(Shell shell, ExecutionEvent event)
    {
        Iterator<WorkspaceSymbolProvider> it =
            TypeScriptCore.LANGUAGE_SERVICE.getWorkspaceSymbolProviders().iterator();
        if (!it.hasNext())
            return null;

        IResource resource = getResource(event);
        if (resource == null)
            return null;

        List<WorkspaceSymbolProvider> providers = new ArrayList<>();
        IProject project = resource.getProject();
        while (it.hasNext())
        {
            WorkspaceSymbolProvider provider = it.next();
            if (project.equals(provider.getContext()))
                providers.add(provider);
        }
        if (providers.isEmpty())
            return null;

        WorkspaceSymbolSelectionDialog dialog = new WorkspaceSymbolSelectionDialog(shell,
            providers.toArray(WorkspaceSymbolProvider[]::new), true);
        dialog.setTitle(MessageFormat.format("Open Symbol in ''{0}''", project.getName()));
        return dialog;
    }

    @Override
    protected Location getLocation(Object item)
    {
        Either<Location, WorkspaceSymbolLocation> location =
            ((WorkspaceSymbolItem)item).getResolvedLocation();

        if (location.isLeft())
            return location.getLeft();

        return new Location(location.getRight().getUri(),
            new Range(new Position(0, 0), new Position(0, 0)));
    }

    private static IResource getResource(ExecutionEvent event)
    {
        IEditorPart part = HandlerUtil.getActiveEditor(event);
        if (part != null)
            return Adapters.adapt(part.getEditorInput(), IResource.class);

        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        return Adapters.adapt(selection.getFirstElement(), IResource.class);
    }
}
