/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.LinkedEditingRangeParams;
import org.eclipse.lsp4j.LinkedEditingRangeRegistrationOptions;
import org.eclipse.lsp4j.LinkedEditingRanges;

/**
 * Provides {@link LinkedEditingRanges} for a given text document position.
 */
public interface LinkedEditingRangeProvider
    extends LanguageFeatureProvider<LinkedEditingRangeRegistrationOptions>
{
    /**
     * Requests linked editing ranges for the given text document position.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<LinkedEditingRanges> getLinkedEditingRanges(LinkedEditingRangeParams params);
}
