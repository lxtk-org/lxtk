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

import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyIncomingCallsParams;
import org.lxtk.CallHierarchyProvider;

/**
 * Requests the incoming calls for the given call hierarchy item.
 */
public class CallHierarchyIncomingCallsRequest
    extends LanguageFeatureRequestWithWorkDoneAndPartialResultProgress<CallHierarchyProvider,
        CallHierarchyIncomingCallsParams, List<CallHierarchyIncomingCall>>
{
    @Override
    protected CompletableFuture<List<CallHierarchyIncomingCall>> send(
        CallHierarchyProvider provider, CallHierarchyIncomingCallsParams params)
    {
        setTitle(MessageFormat.format(Messages.CallHierarchyIncomingCallsRequest_title, params));
        return provider.getCallHierarchyIncomingCalls(params);
    }
}
