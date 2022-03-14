/*******************************************************************************
 * Copyright (c) 2021, 2022 1C-Soft LLC.
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.lxtk.DocumentLinkProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.TaskExecutor;
import org.lxtk.lx4e.requests.DocumentLinkRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of a hyperlink detector that computes hyperlinks
 * using {@link DocumentLinkProvider}s.
 */
public class DocumentHyperlinkDetector
    extends AbstractHyperlinkDetector
{
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
        boolean canShowMultipleHyperlinks)
    {
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return null;

        DocumentLinkResults results = computeDocumentLinkResults(getDocumentLinkProviders(target),
            new DocumentLinkParams(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri())));
        if (results == null)
            return null;

        return createHyperlinks(results, textViewer, region, canShowMultipleHyperlinks);
    }

    /**
     * Returns the current {@link LanguageOperationTarget}.
     *
     * @return the language operation target, or <code>null</code> if none
     */
    public LanguageOperationTarget getLanguageOperationTarget()
    {
        return getAdapter(LanguageOperationTarget.class);
    }

    /**
     * Returns document link providers that match the given target.
     *
     * @param target never <code>null</code>
     * @return the matching document link providers (not <code>null</code>)
     */
    protected DocumentLinkProvider[] getDocumentLinkProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getDocumentLinkProviders(), DocumentLinkProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(DocumentLinkProvider[]::new);
    }

    /**
     * Asks each of the given document link providers to compute a result for the given
     * {@link DocumentLinkParams} and returns the computed results.
     *
     * @param providers never <code>null</code>
     * @param params never <code>null</code>
     * @return the computed document link results, or <code>null</code> if none
     */
    protected DocumentLinkResults computeDocumentLinkResults(DocumentLinkProvider[] providers,
        DocumentLinkParams params)
    {
        if (providers.length == 0)
            return null;

        return new DocumentLinkResults(TaskExecutor.parallelCompute(providers,
            (provider, monitor) -> computeDocumentLinkResult(provider, params, monitor),
            Messages.Computing_links, getDocumentLinkTimeout(), null));
    }

    /**
     * Asks the given document link provider to compute a result for the given
     * {@link DocumentLinkParams} and returns the computed result.
     *
     * @param provider never <code>null</code>
     * @param params never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed document link result, or <code>null</code> if none
     */
    protected DocumentLinkResult computeDocumentLinkResult(DocumentLinkProvider provider,
        DocumentLinkParams params, IProgressMonitor monitor)
    {
        DocumentLinkRequest request = newDocumentLinkRequest();
        request.setProvider(provider);
        // note that request params can get modified as part of request processing
        // (e.g. a progress token can be set); therefore we need to copy the given params
        request.setParams(JsonUtil.deepCopy(params));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
        request.setTimeout(getDocumentLinkTimeout());
        request.setMayThrow(false);

        return new DocumentLinkResult(request.sendAndReceive());
    }

    /**
     * Creates and returns hyperlinks for the given document link results that have been computed
     * for the given region in the given text viewer.
     *
     * @param results never <code>null</code>
     * @param textViewer the text viewer on which the hover popup should be shown
     *  (never <code>null</code>)
     * @param region the hyperlink region (never <code>null</code>)
     * @param canShowMultipleHyperlinks tells whether the caller is able to show multiple hyperlinks
     * @return the created hyperlinks, or <code>null</code> if none
     */
    protected IHyperlink[] createHyperlinks(DocumentLinkResults results, ITextViewer textViewer,
        IRegion region, boolean canShowMultipleHyperlinks)
    {
        for (Map.Entry<DocumentLinkProvider, DocumentLinkResult> entry : results.asMap().entrySet())
        {
            DocumentLinkResult result = entry.getValue();
            if (result != null)
            {
                List<DocumentLink> links = result.getDocumentLinks();
                if (links != null)
                {
                    for (DocumentLink link : links)
                    {
                        IRegion linkRegion;
                        try
                        {
                            linkRegion =
                                DocumentUtil.toRegion(textViewer.getDocument(), link.getRange());
                        }
                        catch (BadLocationException e)
                        {
                            continue;
                        }

                        if (TextUtilities.overlaps(region, linkRegion))
                        {
                            return new IHyperlink[] {
                                createHyperlink(linkRegion, link, entry.getKey()) };
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Creates and returns a hyperlink that covers the given region and
     * opens the given document link.
     *
     * @param region the hyperlink region (never <code>null</code>)
     * @param link the underlying document link (never <code>null</code>)
     * @param provider the provider that created the document link (never <code>null</code>)
     * @return the created hyperlink (not <code>null</code>)
     */
    protected IHyperlink createHyperlink(IRegion region, DocumentLink link,
        DocumentLinkProvider provider)
    {
        return new DocumentHyperlink(region, link, provider);
    }

    /**
     * Returns a new instance of {@link DocumentLinkRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentLinkRequest newDocumentLinkRequest()
    {
        return new DocumentLinkRequest();
    }

    /**
     * Returns the timeout for a document link request.
     *
     * @return a positive duration
     */
    protected Duration getDocumentLinkTimeout()
    {
        return Duration.ofSeconds(1);
    }

    /**
     * Represents a group of {@link DocumentLinkResult}s.
     */
    protected static class DocumentLinkResults
    {
        private final Map<DocumentLinkProvider, DocumentLinkResult> results;

        /**
         * Constructor.
         *
         * @param results not <code>null</code>
         */
        public DocumentLinkResults(Map<DocumentLinkProvider, DocumentLinkResult> results)
        {
            this.results = Objects.requireNonNull(results);
        }

        /**
         * Returns the document link results as a map.
         *
         * @return the document link results as a map (never <code>null</code>)
         */
        public Map<DocumentLinkProvider, DocumentLinkResult> asMap()
        {
            return results;
        }
    }

    /**
     * Represents the result of a document link request.
     */
    protected static class DocumentLinkResult
    {
        private final List<DocumentLink> links;

        /**
         * Constructor.
         *
         * @param links may be <code>null</code>
         */
        public DocumentLinkResult(List<DocumentLink> links)
        {
            this.links = links;
        }

        /**
         * Returns the document link list.
         *
         * @return the document link list, or <code>null</code> if none
         */
        public List<DocumentLink> getDocumentLinks()
        {
            return links;
        }
    }
}
