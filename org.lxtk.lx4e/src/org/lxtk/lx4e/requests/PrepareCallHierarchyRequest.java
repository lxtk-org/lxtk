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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyPrepareParams;
import org.lxtk.CallHierarchyProvider;

/**
 * Requests preparation of a call hierarchy for the symbol at the given text document position.
 */
public class PrepareCallHierarchyRequest
    extends LanguageFeatureRequestWithWorkDoneProgress<CallHierarchyProvider,
        CallHierarchyPrepareParams, List<CallHierarchyItem>>
{
    @Override
    protected CompletableFuture<List<CallHierarchyItem>> send(CallHierarchyProvider provider,
        CallHierarchyPrepareParams params)
    {
        setTitle(MessageFormat.format(Messages.PrepareCallHierarchyRequest_title, params));
        return provider.prepareCallHierarchy(params);
    }
}
