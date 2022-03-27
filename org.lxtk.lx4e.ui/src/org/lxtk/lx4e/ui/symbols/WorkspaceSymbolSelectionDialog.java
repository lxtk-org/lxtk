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
package org.lxtk.lx4e.ui.symbols;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.lxtk.AbstractPartialResultProgress;
import org.lxtk.DocumentUri;
import org.lxtk.WorkspaceSymbolProvider;
import org.lxtk.lx4e.EfsUriHandler;
import org.lxtk.lx4e.IUriHandler;
import org.lxtk.lx4e.ResourceUriHandler;
import org.lxtk.lx4e.UriHandlers;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.TaskExecutor;
import org.lxtk.lx4e.requests.WorkspaceSymbolRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Shows a list of workspace symbols to the user with an input field for a query string
 * used to filter the list of symbols.
 */
public class WorkspaceSymbolSelectionDialog
    extends FilteredItemsSelectionDialog
{
    private static final String DIALOG_SETTINGS = WorkspaceSymbolSelectionDialog.class.getName();

    private static final IUriHandler URI_HANDLER =
        UriHandlers.compose(new ResourceUriHandler(), new EfsUriHandler());

    private final WorkspaceSymbolProvider[] providers;

    /**
     * Creates a new dialog instance.
     *
     * @param shell a parent shell
     * @param providers {@link WorkspaceSymbolProvider}s (not <code>null</code>)
     * @param multi indicates whether the dialog will allow to select more than one item
     */
    public WorkspaceSymbolSelectionDialog(Shell shell, WorkspaceSymbolProvider[] providers,
        boolean multi)
    {
        super(shell, multi);
        for (WorkspaceSymbolProvider provider : providers)
            Objects.requireNonNull(provider);
        this.providers = providers;
        setTitle(Messages.WorkspaceSymbolSelectionDialog_defaultTitle);
        setMessage(Messages.WorkspaceSymbolSelectionDialog_defaultMessage);
        setListLabelProvider(new ListLabelProvider());
        setDetailsLabelProvider(new DetailsLabelProvider());
    }

    /**
     * Creates a new dialog instance.
     *
     * @param shell a parent shell
     * @param provider a {@link WorkspaceSymbolProvider} (not <code>null</code>)
     * @param multi indicates whether the dialog will allow to select more than one item
     */
    public WorkspaceSymbolSelectionDialog(Shell shell, WorkspaceSymbolProvider provider,
        boolean multi)
    {
        this(shell, new WorkspaceSymbolProvider[] { provider }, multi);
    }

    /**
     * Creates a new dialog instance that will not allow to select more than one item.
     *
     * @param shell a parent shell
     * @param provider a {@link WorkspaceSymbolProvider} (not <code>null</code>)
     */
    public WorkspaceSymbolSelectionDialog(Shell shell, WorkspaceSymbolProvider provider)
    {
        this(shell, provider, false);
    }

    @Override
    public String getElementName(Object item)
    {
        return ((SymbolInformation)item).getName();
    }

    @Override
    protected IStatus validateItem(Object item)
    {
        return Status.OK_STATUS;
    }

    /**
     * Returns the workspace symbol providers for this dialog.
     *
     * @return the workspace symbol providers (not <code>null</code>)
     */
    protected WorkspaceSymbolProvider[] getWorkspaceSymbolProviders()
    {
        return providers;
    }

    @Override
    protected void fillContentProvider(AbstractContentProvider contentProvider,
        ItemsFilter itemsFilter, IProgressMonitor monitor) throws CoreException
    {
        TaskExecutor.parallelExecute(getWorkspaceSymbolProviders(),
            (workspaceSymbolProvider, taskMonitor) -> fillContentProvider(contentProvider,
                itemsFilter, workspaceSymbolProvider, taskMonitor),
            Messages.WorkspaceSymbolSelectionDialog_searchTaskName, null, monitor);
    }

    /**
     * Fills the given content provider with symbol information supplied by the given
     * workspace symbol provider.
     *
     * @param contentProvider never <code>null</code>
     * @param itemsFilter never <code>null</code>
     * @param workspaceSymbolProvider never <code>null</code>
     * @param monitor may be <code>null</code>
     */
    protected void fillContentProvider(AbstractContentProvider contentProvider,
        ItemsFilter itemsFilter, WorkspaceSymbolProvider workspaceSymbolProvider,
        IProgressMonitor monitor)
    {
        WorkspaceSymbolRequest request = newWorkspaceSymbolRequest();
        request.setProvider(workspaceSymbolProvider);
        request.setParams(new WorkspaceSymbolParams(itemsFilter.getPattern()));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
        request.setUpPartialResultProgress(
            (() -> new AbstractPartialResultProgress<List<? extends SymbolInformation>>()
            {
                @Override
                protected void onAccept(List<? extends SymbolInformation> items)
                {
                    for (SymbolInformation item : items)
                        contentProvider.add(item, itemsFilter);
                }
            }));
        request.setMayThrow(false);

        List<? extends SymbolInformation> result = request.sendAndReceive();

        if (result == null)
            return;

        for (SymbolInformation item : result)
            contentProvider.add(item, itemsFilter);
    }

    /**
     * Returns a new instance of {@link WorkspaceSymbolRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected WorkspaceSymbolRequest newWorkspaceSymbolRequest()
    {
        return new WorkspaceSymbolRequest();
    }

    @Override
    protected ItemsFilter createFilter()
    {
        return new ItemsFilter()
        {
            @Override
            public boolean matchItem(Object item)
            {
                return matches(getElementName(item));
            }

            @Override
            public boolean isConsistentItem(Object item)
            {
                return true;
            }
        };
    }

    @Override
    protected Comparator getItemsComparator()
    {
        return Comparator.comparing(this::getElementName).thenComparing(this::getUri).thenComparing(
            this::getLine);
    }

    @Override
    protected IDialogSettings getDialogSettings()
    {
        IDialogSettings settings =
            Activator.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

        if (settings == null)
            settings = Activator.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);

        return settings;
    }

    @Override
    protected Control createExtendedContentArea(Composite parent)
    {
        return null;
    }

    private URI getUri(Object item)
    {
        return DocumentUri.convert(((SymbolInformation)item).getLocation().getUri());
    }

    private int getLine(Object item)
    {
        return ((SymbolInformation)item).getLocation().getRange().getStart().getLine();
    }

    private static String getDetails(SymbolInformation info)
    {
        URI uri = DocumentUri.convert(info.getLocation().getUri());
        String location = UriHandlers.toDisplayString(uri, URI_HANDLER);
        String containerName = info.getContainerName();
        if (containerName == null || containerName.isEmpty())
            return location;
        return MessageFormat.format(Messages.WorkspaceSymbolSelectionDialog_detailsPattern,
            containerName, location);
    }

    private class ListLabelProvider
        extends SymbolLabelProvider
    {
        @Override
        public StyledString getStyledText(Object element)
        {
            StyledString ss = super.getStyledText(element);
            if (element instanceof SymbolInformation)
            {
                ss.append(MessageFormat.format(Messages.WorkspaceSymbolSelectionDialog_itemPattern,
                    getDetails((SymbolInformation)element)), StyledString.QUALIFIER_STYLER);
            }
            return ss;
        }

        @Override
        public String getText(Object element)
        {
            return getStyledText(element).getString();
        }
    }

    private static class DetailsLabelProvider
        extends LabelProvider
    {
        @Override
        public String getText(Object element)
        {
            if (element instanceof SymbolInformation)
            {
                return getDetails((SymbolInformation)element);
            }
            return super.getText(element);
        }
    }
}
