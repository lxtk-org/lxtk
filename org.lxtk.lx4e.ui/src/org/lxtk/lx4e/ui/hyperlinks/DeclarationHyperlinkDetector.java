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
import org.eclipse.lsp4j.DeclarationParams;
import org.eclipse.lsp4j.Location;
import org.lxtk.DeclarationProvider;
import org.lxtk.DocumentService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.requests.DeclarationRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of a hyperlink detector that computes hyperlinks
 * using {@link DeclarationProvider}s.
 */
public class DeclarationHyperlinkDetector
    extends AbstractLocationHyperlinkDetector<DeclarationProvider>
{
    @Override
    protected DeclarationProvider[] getLocationLinkProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getDeclarationProviders(), DeclarationProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(DeclarationProvider[]::new);
    }

    @Override
    protected LocationLinkResult computeLocationLinkResult(DeclarationProvider provider,
        LocationLinkContext context, IProgressMonitor monitor)
    {
        DeclarationRequest request = newDeclarationRequest();
        request.setProvider(provider);
        request.setParams(new DeclarationParams(context.getTextDocument(), context.getPosition()));
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
            ? MessageFormat.format(Messages.DeclarationHyperlinkDetector_Hyperlink_text2, index)
            : Messages.DeclarationHyperlinkDetector_Hyperlink_text;
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
            Messages.DeclarationHyperlinkDetector_Hyperlink_text3, new LocationSearchQuery(
                locations, getResultLabel(textViewer, region, locations), documentService));
    }

    @Override
    protected String getResultLabel(ITextViewer textViewer, IRegion region,
        List<? extends Location> locations)
    {
        return MessageFormat.format(Messages.DeclarationHyperlinkDetector_Result_label,
            super.getResultLabel(textViewer, region, locations));
    }

    /**
     * Returns a new instance of {@link DeclarationRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DeclarationRequest newDeclarationRequest()
    {
        return new DeclarationRequest();
    }
}
