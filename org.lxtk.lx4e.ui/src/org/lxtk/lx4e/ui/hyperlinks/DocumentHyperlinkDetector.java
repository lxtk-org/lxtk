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

import java.net.URI;
import java.time.Duration;
import java.util.List;

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
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.requests.DocumentLinkRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of a hyperlink detector that computes hyperlinks
 * using a {@link DocumentLinkProvider}.
 */
public class DocumentHyperlinkDetector
    extends AbstractHyperlinkDetector
{
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
        boolean canShowMultipleHyperlinks)
    {
        LanguageOperationTarget target = getAdapter(LanguageOperationTarget.class);
        if (target == null)
            return null;

        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();
        DocumentLinkProvider provider = languageService.getDocumentMatcher().getBestMatch(
            languageService.getDocumentLinkProviders(), DocumentLinkProvider::getDocumentSelector,
            documentUri, target.getLanguageId());
        if (provider == null)
            return null;

        DocumentLinkRequest request = newDocumentLinkRequest();
        request.setProvider(provider);
        request.setParams(
            new DocumentLinkParams(DocumentUri.toTextDocumentIdentifier(documentUri)));
        request.setUpWorkDoneProgress(
            () -> WorkDoneProgressFactory.newWorkDoneProgressWithJob(false));
        request.setTimeout(getDocumentLinkTimeout());
        request.setMayThrow(false);

        List<DocumentLink> result = request.sendAndReceive();
        if (result == null)
            return null;

        return createHyperlinks(textViewer, region, canShowMultipleHyperlinks, provider, result);
    }

    private IHyperlink[] createHyperlinks(ITextViewer textViewer, IRegion region,
        boolean canShowMultipleHyperlinks, DocumentLinkProvider provider, List<DocumentLink> links)
    {
        for (DocumentLink link : links)
        {
            IRegion linkRegion;
            try
            {
                linkRegion = DocumentUtil.toRegion(textViewer.getDocument(), link.getRange());
            }
            catch (BadLocationException e)
            {
                continue;
            }
            if (TextUtilities.overlaps(region, linkRegion))
                return new IHyperlink[] { createHyperlink(linkRegion, link, provider) };
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
}
