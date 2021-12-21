/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.statushandlers.StatusManager;
import org.lxtk.DocumentLinkProvider;
import org.lxtk.DocumentUri;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.requests.DocumentLinkResolveRequest;
import org.lxtk.lx4e.ui.DefaultEditorHelper;
import org.lxtk.lx4e.ui.EditorHelper;

/**
 * A hyperlink that opens a given {@link DocumentLink}.
 */
public class DocumentHyperlink
    extends AbstractHyperlink
{
    private final DocumentLink link;
    private final DocumentLinkProvider provider;

    /**
     * Constructor.
     *
     * @param region not <code>null</code>
     * @param link not <code>null</code>
     * @param provider not <code>null</code>
     */
    public DocumentHyperlink(IRegion region, DocumentLink link, DocumentLinkProvider provider)
    {
        super(region, link.getTooltip());
        this.link = Objects.requireNonNull(link);
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public void open()
    {
        String target = link.getTarget();
        if (target == null || target.isEmpty())
        {
            if (!Boolean.TRUE.equals(provider.getRegistrationOptions().getResolveProvider()))
                return;

            DocumentLinkResolveRequest request = newDocumentLinkResolveRequest();
            request.setProvider(provider);
            request.setParams(link);
            request.setDefaultResult(link);
            request.setTimeout(getDocumentLinkResolveTimeout());
            request.setMayThrow(false);

            target = request.sendAndReceive().getTarget();
            if (target == null || target.isEmpty())
                return;
        }
        URI uri = DocumentUri.convert(target);
        IFileStore fileStore = null;
        try
        {
            fileStore = EFS.getStore(uri);
        }
        catch (CoreException e)
        {
            // fall through
        }
        if (fileStore != null)
        {
            IFileInfo fileInfo = fileStore.fetchInfo();
            if (fileInfo.exists() && !fileInfo.isDirectory())
            {
                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (window != null)
                {
                    IWorkbenchPage page = window.getActivePage();
                    if (page != null)
                    {
                        EditorHelper editorHelper = getEditorHelper();
                        try
                        {
                            editorHelper.openEditor(page, uri, true);
                        }
                        catch (PartInitException e)
                        {
                            StatusManager.getManager().handle(e.getStatus(),
                                StatusManager.LOG | StatusManager.SHOW);
                        }
                    }
                }
            }
        }
        else // no file store
        {
            try
            {
                IWorkbenchBrowserSupport browserSupport =
                    PlatformUI.getWorkbench().getBrowserSupport();
                IWebBrowser browser = browserSupport.createBrowser(null);
                browser.openURL(uri.toURL());
            }
            catch (PartInitException e)
            {
                StatusManager.getManager().handle(e.getStatus(),
                    StatusManager.LOG | StatusManager.SHOW);
            }
            catch (MalformedURLException e)
            {
                Activator.logError(e);
            }
        }
    }

    /**
     * Returns a new instance of {@link DocumentLinkResolveRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentLinkResolveRequest newDocumentLinkResolveRequest()
    {
        return new DocumentLinkResolveRequest();
    }

    /**
     * Returns the timeout for a document link resolve request.
     *
     * @return a positive duration
     */
    protected Duration getDocumentLinkResolveTimeout()
    {
        return Duration.ofSeconds(1);
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
}
