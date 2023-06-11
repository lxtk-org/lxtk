/*******************************************************************************
 * Copyright (c) 2020, 2023 1C-Soft LLC.
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
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolLocation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
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
import org.lxtk.lx4e.internal.ui.LSPImages;
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
        return ((WorkspaceSymbolItem)item).getName();
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
        request.setUpPartialResultProgress((() -> new AbstractPartialResultProgress<
            Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>>()
        {
            @Override
            protected void onAccept(
                Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result)
            {
                fillContentProvider(contentProvider, itemsFilter, workspaceSymbolProvider, result);
            }
        }));
        request.setMayThrow(false);

        fillContentProvider(contentProvider, itemsFilter, workspaceSymbolProvider,
            request.sendAndReceive());
    }

    private void fillContentProvider(AbstractContentProvider contentProvider,
        ItemsFilter itemsFilter, WorkspaceSymbolProvider workspaceSymbolProvider,
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result)
    {
        if (result == null)
            return;

        if (result.isLeft())
        {
            result.getLeft().forEach(symbol -> contentProvider.add(
                newWorkspaceSymbolItem(Either.forLeft(symbol), workspaceSymbolProvider),
                itemsFilter));
        }
        else if (result.isRight())
        {
            result.getRight().forEach(symbol -> contentProvider.add(
                newWorkspaceSymbolItem(Either.forRight(symbol), workspaceSymbolProvider),
                itemsFilter));
        }
    }

    /**
     * Returns a new content provider item for the given workspace symbol.
     *
     * @param symbol never <code>null</code>
     * @param workspaceSymbolProvider never <code>null</code>
     * @return the created workspace symbol item (not <code>null</code>)
     */
    protected Object newWorkspaceSymbolItem(Either<SymbolInformation, WorkspaceSymbol> symbol,
        WorkspaceSymbolProvider workspaceSymbolProvider)
    {
        WorkspaceSymbolItem workspaceSymbolItem = new WorkspaceSymbolItem(symbol);
        workspaceSymbolItem.setWorkspaceSymbolProvider(workspaceSymbolProvider);
        return workspaceSymbolItem;
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
        return Comparator.comparing(this::getElementName).thenComparing(
            WorkspaceSymbolSelectionDialog::getUri).thenComparing(
                WorkspaceSymbolSelectionDialog::getLine);
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

    private static URI getUri(Object item)
    {
        return DocumentUri.convert(((WorkspaceSymbolItem)item).getLocation().map(Location::getUri,
            WorkspaceSymbolLocation::getUri));
    }

    private static int getLine(Object item)
    {
        Either<Location, WorkspaceSymbolLocation> location =
            ((WorkspaceSymbolItem)item).getLocation();

        if (location.isLeft())
            return location.getLeft().getRange().getStart().getLine();

        return 0;
    }

    private static String getDetails(WorkspaceSymbolItem item)
    {
        URI uri = getUri(item);
        String location = UriHandlers.toDisplayString(uri, URI_HANDLER);
        String containerName = item.getContainerName();
        if (containerName == null || containerName.isEmpty())
            return location;
        return MessageFormat.format(Messages.WorkspaceSymbolSelectionDialog_detailsPattern,
            containerName, location);
    }

    private static class ListLabelProvider
        extends LabelProvider
        implements IStyledLabelProvider
    {
        private static final Styler DEPRECATED_STYLER = new Styler()
        {
            @Override
            public void applyStyles(TextStyle textStyle)
            {
                textStyle.strikeout = true;
            };
        };

        @Override
        public StyledString getStyledText(Object element)
        {
            StyledString ss = new StyledString();
            if (element instanceof WorkspaceSymbolItem)
            {
                WorkspaceSymbolItem item = (WorkspaceSymbolItem)element;

                if (item.isDeprecated())
                    ss.append(item.getName(), DEPRECATED_STYLER);
                else
                    ss.append(item.getName());

                ss.append(MessageFormat.format(Messages.WorkspaceSymbolSelectionDialog_itemPattern,
                    getDetails(item)), StyledString.QUALIFIER_STYLER);
            }
            return ss;
        }

        @Override
        public String getText(Object element)
        {
            return getStyledText(element).getString();
        }

        @Override
        public Image getImage(Object element)
        {
            if (element instanceof WorkspaceSymbolItem)
                return LSPImages.imageFromSymbolKind(((WorkspaceSymbolItem)element).getKind());

            return super.getImage(element);
        }
    }

    private static class DetailsLabelProvider
        extends LabelProvider
    {
        @Override
        public String getText(Object element)
        {
            if (element instanceof WorkspaceSymbolItem)
            {
                return getDetails((WorkspaceSymbolItem)element);
            }
            return super.getText(element);
        }
    }
}
