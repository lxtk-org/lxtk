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
package org.lxtk.lx4e.requests;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.LinkedEditingRangeParams;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.lxtk.LinkedEditingRangeProvider;

/**
 * Requests linked editing ranges for the given text document position.
 */
public class LinkedEditingRangeRequest
    extends LanguageFeatureRequestWithWorkDoneProgress<LinkedEditingRangeProvider,
        LinkedEditingRangeParams, LinkedEditingRanges>
{
    @Override
    protected CompletableFuture<LinkedEditingRanges> send(LinkedEditingRangeProvider provider,
        LinkedEditingRangeParams params)
    {
        setTitle(MessageFormat.format(Messages.LinkedEditingRangeRequest_title, params));
        return provider.getLinkedEditingRanges(params);
    }
}
