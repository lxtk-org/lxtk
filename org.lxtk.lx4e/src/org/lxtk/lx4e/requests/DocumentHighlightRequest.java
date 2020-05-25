/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.requests;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.lxtk.DocumentHighlightProvider;

/**
 * A request for computing document highlights.
 */
public class DocumentHighlightRequest
    extends LanguageFeatureRequest<DocumentHighlightProvider, TextDocumentPositionParams,
        List<? extends DocumentHighlight>>
{
    @Override
    protected Future<List<? extends DocumentHighlight>> send(DocumentHighlightProvider provider,
        TextDocumentPositionParams params)
    {
        setTitle(MessageFormat.format(Messages.DocumentHighlightRequest_title, params));
        return provider.getDocumentHighlights(params);
    }
}
