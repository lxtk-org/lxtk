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
package org.lxtk.lx4e.ui.callhierarchy;

import static org.lxtk.lx4e.UriHandlers.getBuffer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.handly.ui.callhierarchy.CallTextInfo;
import org.eclipse.handly.util.TextRange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyIncomingCallsParams;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.CallHierarchyOutgoingCallsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.CallHierarchyProvider;
import org.lxtk.DefaultWorkDoneProgress;
import org.lxtk.lx4e.CallHierarchyIncomingCallsRequest;
import org.lxtk.lx4e.CallHierarchyOutgoingCallsRequest;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.IUriHandler;
import org.lxtk.lx4e.internal.ui.Activator;

/**
 * Serves as a basis for the implementation of {@link IncomingCallHierarchyNode} and
 * {@link OutgoingCallHierarchyNode} by providing API and default implementation
 * to get the information about calls.
 */
public class CallHierarchyUtility
{
    protected final CallHierarchyProvider provider;
    protected final IUriHandler uriHandler;

    /**
     * Constructor.
     *
     * @param provider not <code>null</code>
     * @param uriHandler not <code>null</code>
     */
    public CallHierarchyUtility(CallHierarchyProvider provider, IUriHandler uriHandler)
    {
        this.provider = Objects.requireNonNull(provider);
        this.uriHandler = Objects.requireNonNull(uriHandler);
    }

    /**
     * Returns the incoming calls for the given call hierarchy item.
     *
     * @param item not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return a list of incoming calls (may be <code>null</code> or empty)
     */
    public List<CallHierarchyIncomingCall> getIncomingCalls(CallHierarchyItem item,
        IProgressMonitor monitor)
    {
        CallHierarchyIncomingCallsRequest request = newIncomingCallsRequest();
        request.setProvider(provider);
        request.setParams(new CallHierarchyIncomingCallsParams(item));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(
            () -> new DefaultWorkDoneProgress(Either.forLeft(UUID.randomUUID().toString())));
        request.setMayThrow(false);
        return request.sendAndReceive();
    }

    /**
     * Returns the outgoing calls for the given call hierarchy item.
     *
     * @param item not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return a list of outgoing calls (may be <code>null</code> or empty)
     */
    public List<CallHierarchyOutgoingCall> getOutgoingCalls(CallHierarchyItem item,
        IProgressMonitor monitor)
    {
        CallHierarchyOutgoingCallsRequest request = newOutgoingCallsRequest();
        request.setProvider(provider);
        request.setParams(new CallHierarchyOutgoingCallsParams(item));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(
            () -> new DefaultWorkDoneProgress(Either.forLeft(UUID.randomUUID().toString())));
        request.setMayThrow(false);
        return request.sendAndReceive();
    }

    /**
     * Returns a list of {@link CallTextInfo} for the given text document ranges.
     * Note that the returned list may be smaller in size than the given list of ranges
     * in case one or more ranges are invalid in the document.
     *
     * @param uri a text document URI (not <code>null</code>)
     * @param ranges call ranges in the given document (not <code>null</code>)
     * @return the requested call text info (never <code>null</code>)
     */
    public List<CallTextInfo> getCallTextInfo(URI uri, List<Range> ranges)
    {
        List<CallTextInfo> result = new ArrayList<>();
        try (IBuffer buffer = getBuffer(uri, uriHandler))
        {
            NonExpiringSnapshot snapshot = new NonExpiringSnapshot(buffer);
            IDocument document = new Document(snapshot.getContents());
            for (Range range : ranges)
            {
                try
                {
                    IRegion r = DocumentUtil.toRegion(document, range);
                    result.add(new CallTextInfo(document.get(r.getOffset(), r.getLength()),
                        new TextRange(r.getOffset(), r.getLength()), range.getStart().getLine(),
                        snapshot.getWrappedSnapshot()));
                }
                catch (BadLocationException e)
                {
                    // ignore: the range is invalid in the document
                }
            }
        }
        catch (CoreException | IllegalStateException e)
        {
            Activator.logError(e);
        }
        return result;
    }

    /**
     * Returns a new instance of {@link CallHierarchyIncomingCallsRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected CallHierarchyIncomingCallsRequest newIncomingCallsRequest()
    {
        return new CallHierarchyIncomingCallsRequest();
    }

    /**
     * Returns a new instance of {@link CallHierarchyOutgoingCallsRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected CallHierarchyOutgoingCallsRequest newOutgoingCallsRequest()
    {
        return new CallHierarchyOutgoingCallsRequest();
    }
}
