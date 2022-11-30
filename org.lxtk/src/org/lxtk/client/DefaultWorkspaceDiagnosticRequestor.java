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

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.lsp4j.DiagnosticServerCancellationData;
import org.eclipse.lsp4j.PreviousResultId;
import org.eclipse.lsp4j.WorkspaceDiagnosticParams;
import org.eclipse.lsp4j.WorkspaceDiagnosticReport;
import org.eclipse.lsp4j.WorkspaceDiagnosticReportPartialResult;
import org.eclipse.lsp4j.WorkspaceDocumentDiagnosticReport;
import org.eclipse.lsp4j.WorkspaceFullDocumentDiagnosticReport;
import org.eclipse.lsp4j.WorkspaceUnchangedDocumentDiagnosticReport;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.lxtk.AbstractPartialResultProgress;
import org.lxtk.DiagnosticProvider;
import org.lxtk.PartialResultProgress;
import org.lxtk.ProgressService;
import org.lxtk.util.DisposableObject;
import org.lxtk.util.Log;

/**
 * Default implementation of the {@link WorkspaceDiagnosticRequestor} interface.
 * <p>
 * This implementation is thread-safe.
 * </p>
 */
public final class DefaultWorkspaceDiagnosticRequestor
    extends DisposableObject
    implements WorkspaceDiagnosticRequestor
{
    private final DiagnosticProvider diagnosticProvider;
    private final Consumer<WorkspaceFullDocumentDiagnosticReport> diagnosticConsumer;
    private final Log log;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final Map<String, String> resultIds = new HashMap<>();
    private final RequestData request = new RequestData();
    private AutoRetriggerData autoRetrigger;
    private Function<Throwable, Boolean> errorPolicy;

    /**
     * Constructor.
     *
     * @param diagnosticProvider not <code>null</code>, must support workspace diagnostics
     * @param diagnosticConsumer not <code>null</code>
     * @param log not <code>null</code>
     */
    public DefaultWorkspaceDiagnosticRequestor(DiagnosticProvider diagnosticProvider,
        Consumer<WorkspaceFullDocumentDiagnosticReport> diagnosticConsumer, Log log)
    {
        this.diagnosticProvider = Objects.requireNonNull(diagnosticProvider);
        this.diagnosticConsumer = Objects.requireNonNull(diagnosticConsumer);
        this.log = Objects.requireNonNull(log);

        if (!diagnosticProvider.getRegistrationOptions().isWorkspaceDiagnostics())
            throw new IllegalArgumentException();
    }

    /**
     * Sets an optional delay for auto-retriggering workspace diagnostic pull requests.
     * Auto-retriggering will be disabled if no delay is set.
     * By default, no delay is set for auto-retriggering.
     *
     * @param delay a delay for auto-retriggering
     */
    public synchronized void setAutoRetrigger(Duration delay)
    {
        if (delay != null)
        {
            if (autoRetrigger == null)
                autoRetrigger = new AutoRetriggerData();

            autoRetrigger.delay = delay.toMillis();
        }
        else if (autoRetrigger != null)
        {
            if (autoRetrigger.future != null)
            {
                autoRetrigger.future.cancel(false);
                autoRetrigger.future = null;
            }
            autoRetrigger = null;
        }
    }

    /**
     * Sets an optional policy for handling workspace diagnostic response errors (except for
     * cancellation errors). If the policy returns <code>false</code>, this requestor will no longer
     * make requests to its diagnostic provider. Note that the error will have already been logged
     * before the policy is applied. As a rule, the policy should not throw exceptions. By default,
     * no error policy is set.
     *
     * @param errorPolicy an error policy
     */
    public synchronized void setErrorPolicy(Function<Throwable, Boolean> errorPolicy)
    {
        this.errorPolicy = errorPolicy;
    }

    @Override
    public synchronized void dispose()
    {
        try
        {
            resultIds.clear();
            if (request.future != null)
            {
                request.retrigger = false;
                request.future.cancel(true);
                request.future = null;
            }
            if (autoRetrigger != null)
            {
                if (autoRetrigger.future != null)
                {
                    autoRetrigger.future.cancel(false);
                    autoRetrigger.future = null;
                }
                autoRetrigger = null;
            }
            errorPolicy = null;
            executor.shutdown();
        }
        finally
        {
            super.dispose();
        }
    }

    @Override
    public synchronized void triggerWorkspacePull()
    {
        if (isDisposed())
            return;

        if (request.future != null)
        {
            request.retrigger = true;
            request.future.cancel(true);
        }
        else
        {
            if (autoRetrigger != null)
            {
                if (autoRetrigger.future != null)
                {
                    autoRetrigger.future.cancel(false);
                    autoRetrigger.future = null;
                }
            }

            WorkspaceDiagnosticParams params =
                new WorkspaceDiagnosticParams(getPreviousResultIds());
            params.setIdentifier(diagnosticProvider.getRegistrationOptions().getIdentifier());

            PartialResultProgress partialResultProgress = null;
            ProgressService progressService = diagnosticProvider.getProgressService();
            if (progressService != null)
            {
                partialResultProgress =
                    new AbstractPartialResultProgress<WorkspaceDiagnosticReportPartialResult>()
                    {
                        @Override
                        protected void onAccept(WorkspaceDiagnosticReportPartialResult result)
                        {
                            executor.execute(() ->
                            {
                                try
                                {
                                    synchronized (DefaultWorkspaceDiagnosticRequestor.this)
                                    {
                                        if (isDisposed())
                                            return;

                                        processWorkspaceDiagnosticReport(result.getItems());
                                    }
                                }
                                catch (Throwable t)
                                {
                                    log.error(Messages.getString(
                                        "DefaultWorkspaceDiagnosticRequestor.Error.PartialResultProcessingFailed"), //$NON-NLS-1$
                                        t);
                                }
                            });
                        }
                    };
                progressService.attachProgress(partialResultProgress);
                params.setPartialResultToken(partialResultProgress.getToken());
            }

            request.future = diagnosticProvider.getWorkspaceDiagnostics(params);

            if (partialResultProgress != null)
                partialResultProgress.connectWith(request.future);

            request.future.whenCompleteAsync((WorkspaceDiagnosticReport result, Throwable e) ->
            {
                try
                {
                    synchronized (DefaultWorkspaceDiagnosticRequestor.this)
                    {
                        if (isDisposed())
                            return;

                        try
                        {
                            if (result != null)
                            {
                                processWorkspaceDiagnosticReport(result.getItems());
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
                                            DefaultDiagnosticRequestor.getDiagnosticServerCancellationData(
                                                responseError.getData());
                                        if (data == null || data.isRetriggerRequest())
                                            request.retrigger = true;
                                        return;
                                    }
                                }

                                log.error(
                                    Messages.getString(
                                        "DefaultWorkspaceDiagnosticRequestor.Error.RequestFailed"), //$NON-NLS-1$
                                    e);

                                if (errorPolicy != null
                                    && Boolean.FALSE.equals(errorPolicy.apply(e)))
                                {
                                    log.error(Messages.getString(
                                        "DefaultWorkspaceDiagnosticRequestor.Error.DisabledDueToErrors")); //$NON-NLS-1$
                                    dispose();
                                }
                            }
                        }
                        finally
                        {
                            request.future = null;
                            if (request.retrigger)
                            {
                                request.retrigger = false;
                                try
                                {
                                    triggerWorkspacePull();
                                }
                                catch (Throwable t)
                                {
                                    log.error(Messages.getString(
                                        "DefaultWorkspaceDiagnosticRequestor.Error.FailedToRetriggerRequest"), //$NON-NLS-1$
                                        t);
                                }
                            }
                            else if (autoRetrigger != null)
                            {
                                autoRetrigger.future = executor.schedule(() ->
                                {
                                    try
                                    {
                                        triggerWorkspacePull();
                                    }
                                    catch (Throwable t)
                                    {
                                        log.error(Messages.getString(
                                            "DefaultWorkspaceDiagnosticRequestor.Error.FailedToAutoRetriggerRequest"), //$NON-NLS-1$
                                            t);
                                    }
                                }, autoRetrigger.delay, TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                }
                catch (Throwable t)
                {
                    log.error(
                        Messages.getString(
                            "DefaultWorkspaceDiagnosticRequestor.Error.ResponseProcessingFailed"), //$NON-NLS-1$
                        t);
                }
            }, executor);
        }
    }

    private void processWorkspaceDiagnosticReport(List<WorkspaceDocumentDiagnosticReport> items)
    {
        for (WorkspaceDocumentDiagnosticReport item : items)
        {
            if (item.isLeft())
            {
                WorkspaceFullDocumentDiagnosticReport report = item.getLeft();
                try
                {
                    diagnosticConsumer.accept(report);
                    resultIds.put(report.getUri(), report.getResultId());
                }
                catch (Throwable t)
                {
                    log.error(MessageFormat.format(
                        Messages.getString(
                            "DefaultWorkspaceDiagnosticRequestor.Error.AcceptingDiagnosticsFailed"), //$NON-NLS-1$
                        report.getUri()), t);
                }
            }
            else if (item.isRight())
            {
                WorkspaceUnchangedDocumentDiagnosticReport report = item.getRight();
                resultIds.put(report.getUri(), report.getResultId());
            }
        }
    }

    private List<PreviousResultId> getPreviousResultIds()
    {
        List<PreviousResultId> previousResultIds = new ArrayList<>();
        resultIds.forEach((uri, value) ->
        {
            if (value != null)
                previousResultIds.add(new PreviousResultId(uri, value));
        });
        return previousResultIds;
    }

    private static class RequestData
    {
        CompletableFuture<WorkspaceDiagnosticReport> future;
        boolean retrigger;
    }

    private static class AutoRetriggerData
    {
        long delay;
        Future<?> future;
    }
}
