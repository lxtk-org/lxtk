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
package org.lxtk;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.TextEdit;

/**
 * Provides {@link TextEdit}s for formatting a given text document range.
 *
 * @see LanguageService
 */
public interface DocumentRangeFormattingProvider
    extends LanguageFeatureProvider
{
    /**
     * Requests formatting edits for the given {@link DocumentRangeFormattingParams}.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<? extends TextEdit>> getRangeFormattingEdits(
        DocumentRangeFormattingParams params);
}
