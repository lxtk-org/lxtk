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

import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.TextEdit;
import org.lxtk.DocumentFormattingProvider;

/**
 * A request for computing document formatting edits.
 */
public class DocumentFormattingRequest
    extends LanguageFeatureRequest<DocumentFormattingProvider,
        DocumentFormattingParams, List<? extends TextEdit>>
{
    @Override
    protected Future<List<? extends TextEdit>> send(
        DocumentFormattingProvider provider, DocumentFormattingParams params)
    {
        setTitle(MessageFormat.format(Messages.DocumentFormattingRequest_title,
            params));
        return provider.getFormattingEdits(params);
    }
}
