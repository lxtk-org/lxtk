/*******************************************************************************
 * Copyright (c) 2019, 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.codeaction;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.lxtk.CodeActionProvider;
import org.lxtk.CommandHandler;
import org.lxtk.CommandService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.ProgressService;
import org.lxtk.WorkDoneProgress;
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.RefactoringExecutor;
import org.lxtk.lx4e.refactoring.WorkspaceEditRefactoring;
import org.lxtk.lx4e.requests.CodeActionRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

class CodeActions
{
    static boolean hasCodeActionProvider(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getFirstMatch(
            languageService.getCodeActionProviders(), CodeActionProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()) != null;
    }

    static CodeActionProvider[] getCodeActionProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getCodeActionProviders(), CodeActionProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(CodeActionProvider[]::new);
    }

    static CodeActionResults computeCodeActionResults(CodeActionProvider[] providers,
        CodeActionParams params, Supplier<CodeActionRequest> requestSupplier, Duration timeout)
    {
        if (providers.length == 0)
            return null;

        Map<CodeActionProvider, CodeActionResult> results = new LinkedHashMap<>();

        for (CodeActionProvider provider : providers)
            results.put(provider, null); // initialize iteration order

        JobGroup jobGroup =
            new JobGroup(Messages.CodeActions_Computing_code_actions, 0, providers.length);

        for (CodeActionProvider provider : providers)
        {
            Job job = Job.create(Messages.CodeActions_Computing_code_actions, monitor ->
            {
                CodeActionRequest request = requestSupplier.get();
                request.setProvider(provider);
                // note that request params can get modified as part of request processing
                // (e.g. a progress token can be set); therefore we need to copy the given params
                request.setParams(JsonUtil.deepCopy(params));
                request.setTimeout(timeout);
                request.setMayThrow(false);
                request.setProgressMonitor(monitor);
                request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);

                List<Either<Command, CodeAction>> codeActions = request.sendAndReceive();

                synchronized (results)
                {
                    results.put(provider, new CodeActionResult(codeActions));
                }
            });
            job.setJobGroup(jobGroup);
            job.schedule();
        }

        try
        {
            jobGroup.join(0, null);
        }
        catch (InterruptedException e)
        {
            // ignore
        }

        return new CodeActionResults(results);
    }

    static void execute(Command command, String label, CommandService commandService)
    {
        doExecute(command, label, commandService).exceptionally(e ->
        {
            e = Activator.unwrap(e);
            if (!Activator.isCancellation(e))
            {
                StatusManager.getManager().handle(
                    Activator.createErrorStatus(
                        MessageFormat.format(Messages.CodeActions_Execution_error, label), e),
                    StatusManager.LOG | StatusManager.SHOW);
            }
            return null;
        });
    }

    static void execute(CodeAction codeAction, String label,
        IWorkspaceEditChangeFactory workspaceEditChangeFactory, CodeActionProvider provider)
    {
        resolveIfNecessary(codeAction, provider).thenCompose(resolvedCodeAction ->
        {
            CompletableFuture<?> future = new CompletableFuture<>();

            WorkspaceEdit edit = resolvedCodeAction.getEdit();
            if (edit == null)
                future.complete(null);
            else
            {
                WorkspaceEditRefactoring refactoring =
                    new WorkspaceEditRefactoring(label, workspaceEditChangeFactory);
                refactoring.setWorkspaceEdit(edit);

                PlatformUI.getWorkbench().getDisplay().asyncExec(() ->
                {
                    try
                    {
                        RefactoringStatus status =
                            RefactoringExecutor.execute(refactoring, getShell());
                        if (status.hasFatalError())
                            throw new CoreException(
                                status.getEntryWithHighestSeverity().toStatus());

                        future.complete(null);
                    }
                    catch (Throwable e)
                    {
                        future.completeExceptionally(e);
                    }
                });
            }
            return future;

        }).thenCompose(x -> doExecute(codeAction.getCommand(), null,
            provider.getCommandService())).exceptionally(e ->
            {
                e = Activator.unwrap(e);
                if (!Activator.isCancellation(e))
                {
                    StatusManager.getManager().handle(
                        Activator.createErrorStatus(
                            MessageFormat.format(Messages.CodeActions_Execution_error, label), e),
                        StatusManager.LOG | StatusManager.SHOW);
                }
                return null;
            });
    }

    private static CompletableFuture<CodeAction> resolveIfNecessary(CodeAction codeAction,
        CodeActionProvider provider)
    {
        if (codeAction.getEdit() != null
            || !Boolean.TRUE.equals(provider.getRegistrationOptions().getResolveProvider()))
            return CompletableFuture.completedFuture(codeAction);

        return provider.resolveCodeAction(codeAction);
    }

    private static CompletableFuture<Object> doExecute(Command command, String label,
        CommandService commandService)
    {
        if (command == null)
            return CompletableFuture.completedFuture(null);

        if (label == null)
            label = command.getTitle();

        CommandHandler handler = commandService.getCommandHandler(command.getCommand());
        if (handler == null)
        {
            CompletableFuture<Object> result = new CompletableFuture<>();
            result.completeExceptionally(new CoreException(Activator.createErrorStatus(
                MessageFormat.format(Messages.CodeActions_Command_not_available, label), null)));
            return result;
        }

        ExecuteCommandParams params =
            new ExecuteCommandParams(command.getCommand(), command.getArguments());

        WorkDoneProgress workDoneProgress = null;
        if (Boolean.TRUE.equals(handler.getRegistrationOptions().getWorkDoneProgress()))
        {
            ProgressService progressService = handler.getProgressService();
            if (progressService != null)
            {
                workDoneProgress = WorkDoneProgressFactory.newWorkDoneProgressWithJob(false);
                progressService.attachProgress(workDoneProgress);
                params.setWorkDoneToken(workDoneProgress.getToken());
            }
        }

        CompletableFuture<Object> result = handler.execute(params);

        if (workDoneProgress != null)
            workDoneProgress.connectWith(result);

        return result;
    }

    private static Shell getShell()
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            return null;
        return window.getShell();
    }

    private CodeActions()
    {
    }
}
