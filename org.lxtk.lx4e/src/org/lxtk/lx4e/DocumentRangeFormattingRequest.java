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
package org.lxtk.lx4e;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.TextEdit;
import org.lxtk.DocumentRangeFormattingProvider;

/**
 * Requests formatting edits for the given {@link DocumentRangeFormattingParams}.
 */
public class DocumentRangeFormattingRequest
    extends LanguageFeatureRequest<DocumentRangeFormattingProvider, DocumentRangeFormattingParams,
        List<? extends TextEdit>>
{
    @Override
    protected CompletableFuture<List<? extends TextEdit>> send(
        DocumentRangeFormattingProvider provider, DocumentRangeFormattingParams params)
    {
        setTitle(MessageFormat.format(Messages.DocumentRangeFormattingRequest_title, params));
        return provider.getRangeFormattingEdits(params);
    }
}
