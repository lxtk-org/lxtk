/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
package org.lxtk.client;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticServerCancellationData;
import org.eclipse.lsp4j.DocumentDiagnosticParams;
import org.eclipse.lsp4j.DocumentDiagnosticReport;
import org.eclipse.lsp4j.RelatedFullDocumentDiagnosticReport;
import org.eclipse.lsp4j.RelatedUnchangedDocumentDiagnosticReport;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.lxtk.DefaultDocumentMatcher;
import org.lxtk.DiagnosticProvider;
import org.lxtk.DocumentMatcher;
import org.lxtk.DocumentUri;
import org.lxtk.TextDocument;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.util.DisposableObject;
import org.lxtk.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Default implementation of the {@link DiagnosticRequestor} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DefaultDiagnosticRequestor
    extends DisposableObject
    implements DiagnosticRequestor
{
    private final DiagnosticProvider diagnosticProvider;
    private final BiConsumer<URI, Collection<Diagnostic>> diagnosticConsumer;
    private final Log log;
    private final Map<URI, SequenceData> sequences = new HashMap<>();
    private DocumentMatcher documentMatcher = DefaultDocumentMatcher.INSTANCE;

    /**
     * Constructor.
     *
     * @param diagnosticProvider not <code>null</code>
     * @param diagnosticConsumer not <code>null</code>
     * @param log not <code>null</code>
     */
    public DefaultDiagnosticRequestor(DiagnosticProvider diagnosticProvider,
        BiConsumer<URI, Collection<Diagnostic>> diagnosticConsumer, Log log)
    {
        this.diagnosticProvider = Objects.requireNonNull(diagnosticProvider);
        this.diagnosticConsumer = Objects.requireNonNull(diagnosticConsumer);
        this.log = Objects.requireNonNull(log);
    }

    /**
     * Sets the document matcher for this diagnostic requestor.
     *
     * @param documentMatcher not <code>null</code>
     */
    public void setDocumentMatcher(DocumentMatcher documentMatcher)
    {
        this.documentMatcher = Objects.requireNonNull(documentMatcher);
    }

    @Override
    public synchronized void dispose()
    {
        try
        {
            for (SequenceData sequence : sequences.values())
            {
                RequestData request = sequence.request;
                if (request.future != null)
                {
                    request.retrigger = false;
                    request.future.cancel(true);
                }
            }
            sequences.clear();
        }
        finally
        {
            super.dispose();
        }
    }

    @Override
    public synchronized void endDocumentPullSequence(TextDocument document)
    {
        SequenceData sequence = sequences.remove(document.getUri());
        if (sequence != null)
        {
            RequestData request = sequence.request;
            if (request.future != null)
            {
                request.retrigger = false;
                request.future.cancel(true);
            }
        }
    }

    @Override
    public synchronized void cancelDocumentPull(TextDocument document)
    {
        SequenceData sequence = sequences.get(document.getUri());
        if (sequence != null)
        {
            RequestData request = sequence.request;
            if (request.future != null)
            {
                request.retrigger = false;
                request.future.cancel(true);
            }
        }
    }

    @Override
    public synchronized void triggerDocumentPull(TextDocument document, TriggeringContext context)
    {
        if (isDisposed())
            return;

        TriggeringReason reason = context != null ? context.getTriggeringReason() : null;

        if (TriggeringReason.INTERFILE_CHANGE.equals(reason)
            && !diagnosticProvider.getRegistrationOptions().isInterFileDependencies())
            return;

        URI documentUri = document.getUri();

        SequenceData sequence = sequences.computeIfAbsent(documentUri,
            k -> documentMatcher.isMatch(
                diagnosticProvider.getRegistrationOptions().getDocumentSelector(), documentUri,
                document.getLanguageId()) ? new SequenceData() : null);
        if (sequence == null)
            return;

        RequestData request = sequence.request;
        if (request.future == null)
        {
            triggerPullRequest(documentUri, sequence);
        }
        else
        {
            if (!TriggeringReason.DID_BECOME_ACTIVE.equals(reason)
                && !TriggeringReason.DID_BECOME_VISIBLE.equals(reason))
            {
                request.retrigger = true;
                request.future.cancel(true);
            }
        }
    }

    private synchronized void triggerPullRequest(URI documentUri, SequenceData sequence)
    {
        RequestData request = sequence.request;

        if (request.future != null)
            throw new AssertionError();

        DocumentDiagnosticParams params = new DocumentDiagnosticParams(
            new TextDocumentIdentifier(DocumentUri.convert(documentUri)));
        params.setIdentifier(diagnosticProvider.getRegistrationOptions().getIdentifier());
        params.setPreviousResultId(sequence.resultId);

        request.future = diagnosticProvider.getDocumentDiagnostics(params);
        request.future.whenCompleteAsync((DocumentDiagnosticReport result, Throwable e) ->
        {
            synchronized (this)
            {
                if (sequences.get(documentUri) != sequence)
                    return;

                try
                {
                    if (result != null)
                    {
                        if (result.isLeft())
                        {
                            RelatedFullDocumentDiagnosticReport report = result.getLeft();
                            sequence.resultId = report.getResultId();
                            diagnosticConsumer.accept(documentUri, report.getItems());
                        }
                        else if (result.isRight())
                        {
                            RelatedUnchangedDocumentDiagnosticReport report = result.getRight();
                            sequence.resultId = report.getResultId();
                        }
                    }
                    else if (e != null)
                    {
                        if (e instanceof CancellationException)
                            return;

                        if (e instanceof ResponseErrorException)
                        {
                            ResponseError responseError =
                                ((ResponseErrorException)e).getResponseError();

                            if (responseError.getCode() == ResponseErrorCode.RequestCancelled.getValue())
                                return;

                            if (responseError.getCode() == ResponseErrorCode.ServerCancelled.getValue())
                            {
                                DiagnosticServerCancellationData data =
                                    getDiagnosticServerCancellationData(responseError.getData());
                                if (data == null || data.isRetriggerRequest())
                                    request.retrigger = true;
                                return;
                            }
                        }

                        log.error(MessageFormat.format(
                            Messages.getString("DefaultDiagnosticRequestor.Error.RequestFailed"), //$NON-NLS-1$
                            documentUri), e);
                    }
                }
                finally
                {
                    request.future = null;
                    if (request.retrigger)
                    {
                        request.retrigger = false;
                        triggerPullRequest(documentUri, sequence);
                    }
                }
            }
        });
    }

    private static DiagnosticServerCancellationData getDiagnosticServerCancellationData(Object data)
    {
        if (data instanceof DiagnosticServerCancellationData)
            return (DiagnosticServerCancellationData)data;

        if (data instanceof JsonElement)
        {
            try
            {
                return DefaultGson.INSTANCE.fromJson((JsonElement)data,
                    DiagnosticServerCancellationData.class);
            }
            catch (JsonParseException e)
            {
                // ignore
            }
        }

        return null;
    }

    private static class SequenceData
    {
        String resultId;
        final RequestData request = new RequestData();
    }

    private static class RequestData
    {
        CompletableFuture<DocumentDiagnosticReport> future;
        boolean retrigger;
    }
}
