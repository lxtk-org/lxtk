/*******************************************************************************
 * Copyright (c) 2019, 2022 1C-Soft LLC.
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
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.lxtk.DefinitionProvider;
import org.lxtk.DocumentService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.requests.DefinitionRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of a hyperlink detector that computes hyperlinks
 * using {@link DefinitionProvider}(s).
 */
public class DefinitionHyperlinkDetector
    extends AbstractLocationHyperlinkDetector<DefinitionProvider>
{
    @Override
    protected DefinitionProvider[] getLocationLinkProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getDefinitionProviders(), DefinitionProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(DefinitionProvider[]::new);
    }

    @Override
    protected LocationLinkResult computeLocationLinkResult(DefinitionProvider provider,
        LocationLinkContext context, IProgressMonitor monitor)
    {
        DefinitionRequest request = newDefinitionRequest();
        request.setProvider(provider);
        request.setParams(new DefinitionParams(context.getTextDocument(), context.getPosition()));
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
            ? MessageFormat.format(Messages.DefinitionHyperlinkDetector_Hyperlink_text2, index)
            : Messages.DefinitionHyperlinkDetector_Hyperlink_text;
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
            Messages.DefinitionHyperlinkDetector_Hyperlink_text3, new LocationSearchQuery(locations,
                getResultLabel(textViewer, region, locations), documentService));
    }

    @Override
    protected String getResultLabel(ITextViewer textViewer, IRegion region,
        List<? extends Location> locations)
    {
        return MessageFormat.format(Messages.DefinitionHyperlinkDetector_Result_label,
            super.getResultLabel(textViewer, region, locations));
    }

    /**
     * Returns a new instance of {@link DefinitionRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DefinitionRequest newDefinitionRequest()
    {
        return new DefinitionRequest();
    }
}
