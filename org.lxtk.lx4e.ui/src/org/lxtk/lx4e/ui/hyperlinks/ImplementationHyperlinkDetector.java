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
package org.lxtk.lx4e.ui.hyperlinks;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.lsp4j.ImplementationParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentService;
import org.lxtk.DocumentUri;
import org.lxtk.ImplementationProvider;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.requests.ImplementationRequest;
import org.lxtk.lx4e.requests.Request;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of a hyperlink detector that computes hyperlinks
 * using an {@link ImplementationProvider}.
 */
public class ImplementationHyperlinkDetector
    extends AbstractLocationHyperlinkDetector
{
    @Override
    protected Request<
        Either<List<? extends Location>, List<? extends LocationLink>>> createHyperlinkRequest(
            LanguageOperationTarget target, Position position)
    {
        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();
        ImplementationProvider provider = languageService.getDocumentMatcher().getBestMatch(
            languageService.getImplementationProviders(),
            ImplementationProvider::getDocumentSelector, documentUri, target.getLanguageId());
        if (provider == null)
            return null;

        ImplementationRequest request = newImplementationRequest();
        request.setProvider(provider);
        request.setParams(
            new ImplementationParams(DocumentUri.toTextDocumentIdentifier(documentUri), position));
        request.setUpWorkDoneProgress(
            () -> WorkDoneProgressFactory.newWorkDoneProgressWithJob(false));
        return request;
    }

    @Override
    protected IHyperlink createHyperlink(IRegion region, Location location, int index)
    {
        String text = (++index > 0)
            ? MessageFormat.format(Messages.ImplementationHyperlinkDetector_Hyperlink_text2, index)
            : Messages.ImplementationHyperlinkDetector_Hyperlink_text;
        return new LocationHyperlink(region, text, location);
    }

    @Override
    protected IHyperlink createHyperlink(ITextViewer textViewer, IRegion region,
        List<? extends Location> locations)
    {
        DocumentService documentService = getAdapter(DocumentService.class);
        if (documentService == null)
            return null;

        return new ShowSearchResultHyperlink(region,
            Messages.ImplementationHyperlinkDetector_Hyperlink_text3, new LocationSearchQuery(
                locations, getResultLabel(textViewer, region, locations), documentService));
    }

    @Override
    protected String getResultLabel(ITextViewer textViewer, IRegion region,
        List<? extends Location> locations)
    {
        return MessageFormat.format(Messages.ImplementationHyperlinkDetector_Result_label,
            super.getResultLabel(textViewer, region, locations));
    }

    /**
     * Returns a new instance of {@link ImplementationRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected ImplementationRequest newImplementationRequest()
    {
        return new ImplementationRequest();
    }
}
