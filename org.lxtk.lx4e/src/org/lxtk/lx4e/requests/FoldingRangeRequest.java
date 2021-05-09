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
package org.lxtk.lx4e.requests;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.lxtk.FoldingRangeProvider;

/**
 * Requests folding ranges found in the given text document.
 */
public class FoldingRangeRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<FoldingRangeProvider,
        FoldingRangeRequestParams, List<FoldingRange>>
{
    @Override
    protected CompletableFuture<List<FoldingRange>> send(FoldingRangeProvider provider,
        FoldingRangeRequestParams params)
    {
        setTitle(MessageFormat.format(Messages.FoldingRangeRequest_title, params));
        return provider.getFoldingRanges(params);
    }
}
