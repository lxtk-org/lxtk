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
package org.lxtk.lx4e.ui.hyperlinks;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.lsp4j.ImplementationParams;
import org.eclipse.lsp4j.Location;
import org.lxtk.DocumentService;
import org.lxtk.ImplementationProvider;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.requests.ImplementationRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of a hyperlink detector that computes hyperlinks
 * using {@link ImplementationProvider}(s).
 */
public class ImplementationHyperlinkDetector
    extends AbstractLocationHyperlinkDetector<ImplementationProvider>
{
    @Override
    protected ImplementationProvider[] getLocationLinkProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getImplementationProviders(),
            ImplementationProvider::getDocumentSelector, target.getDocumentUri(),
            target.getLanguageId()).toArray(ImplementationProvider[]::new);
    }

    @Override
    protected LocationLinkResult computeLocationLinkResult(ImplementationProvider provider,
        LocationLinkContext context, IProgressMonitor monitor)
    {
        ImplementationRequest request = newImplementationRequest();
        request.setProvider(provider);
        request.setParams(
            new ImplementationParams(context.getTextDocument(), context.getPosition()));
        request.setTimeout(getHyperlinkTimeout());
        request.setMayThrow(false);
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
        return new LocationLinkResult(request.sendAndReceive());
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
