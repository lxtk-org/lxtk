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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyIncomingCallsParams;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.CallHierarchyOutgoingCallsParams;
import org.eclipse.lsp4j.CallHierarchyPrepareParams;
import org.eclipse.lsp4j.CallHierarchyRegistrationOptions;

/**
 * Provides a call hierarchy for the symbol at a given text document position.
 *
 * @see LanguageService
 */
public interface CallHierarchyProvider
    extends LanguageFeatureProvider<CallHierarchyRegistrationOptions>
{
    /**
     * Requests preparation of a call hierarchy for the symbol at the given text document position.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<CallHierarchyItem>> prepareCallHierarchy(
        CallHierarchyPrepareParams params);

    /**
     * Requests the incoming calls for the given call hierarchy item.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<CallHierarchyIncomingCall>> getCallHierarchyIncomingCalls(
        CallHierarchyIncomingCallsParams params);

    /**
     * Requests the outgoing calls for the given call hierarchy item.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<List<CallHierarchyOutgoingCall>> getCallHierarchyOutgoingCalls(
        CallHierarchyOutgoingCallsParams params);
}
