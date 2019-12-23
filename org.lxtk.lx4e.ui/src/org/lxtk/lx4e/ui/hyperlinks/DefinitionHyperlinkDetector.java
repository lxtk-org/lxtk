/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DefinitionProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;

/**
 * TODO JavaDoc
 */
public class DefinitionHyperlinkDetector
    extends AbstractLocationHyperlinkDetector
{
    @Override
    protected CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> requestLocationInfo(
        LanguageOperationTarget target, Position position)
    {
        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();
        DefinitionProvider provider =
            languageService.getDocumentMatcher().getBestMatch(
                languageService.getDefinitionProviders(),
                DefinitionProvider::getDocumentSelector, documentUri,
                target.getLanguageId());
        if (provider == null)
            return null;
        return provider.getDefinition(new TextDocumentPositionParams(
            DocumentUri.toTextDocumentIdentifier(documentUri), position));
    }

    @Override
    protected IHyperlink createHyperlink(IRegion region, Location location,
        int index)
    {
        String text = Messages.DefinitionHyperlinkDetector_Hyperlink_text;
        if (++index > 0)
            text += " " + index; //$NON-NLS-1$
        return new LocationHyperlink(region, text, location);
    }
}