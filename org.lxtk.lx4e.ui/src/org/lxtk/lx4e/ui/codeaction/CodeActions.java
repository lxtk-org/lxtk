/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.statushandlers.StatusManager;
import org.lxtk.CodeActionProvider;
import org.lxtk.CommandService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.refactoring.WorkspaceEditRefactoring;

class CodeActions
{
    static boolean hasCodeActionProvider(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getFirstMatch(
            languageService.getCodeActionProviders(), CodeActionProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()) != null;
    }

    static CodeActionProvider getCodeActionProvider(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getBestMatch(
            languageService.getCodeActionProviders(), CodeActionProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId());
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

    // This method must be called in the UI thread.
    static void execute(CodeAction codeAction, String label,
        WorkspaceEditChangeFactory workspaceEditChangeFactory, CodeActionProvider provider)
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

                PerformRefactoringOperation operation = new PerformRefactoringOperation(refactoring,
                    CheckConditionsOperation.ALL_CONDITIONS);

                // Note that it is important to execute the refactoring's change in the UI thread.
                // Otherwise, there might be timing issues, e.g. incorrect modification stamps.
                // See also org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper.
                try
                {
                    ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
                    // Note that WorkspaceModifyOperation is used to get EventLoopProgressMonitor.
                    dialog.run(false, false, new WorkspaceModifyOperation()
                    {
                        @Override
                        protected void execute(IProgressMonitor monitor)
                            throws CoreException, InvocationTargetException, InterruptedException
                        {
                            try
                            {
                                operation.run(monitor);

                                RefactoringStatus status = operation.getConditionStatus();
                                status.merge(operation.getValidationStatus());
                                if (status.hasFatalError())
                                    throw new CoreException(
                                        status.getEntryWithHighestSeverity().toStatus());

                                future.complete(null);
                            }
                            catch (Throwable e)
                            {
                                future.completeExceptionally(e);
                            }
                        }
                    });
                }
                catch (InvocationTargetException | InterruptedException e) // should not happen
                {
                    future.completeExceptionally(e);
                }
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
        if (codeAction.getEdit() != null || !provider.supportsResolveCodeAction())
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

        CompletableFuture<Object> result =
            commandService.executeCommand(command.getCommand(), command.getArguments());
        if (result == null)
        {
            result = new CompletableFuture<>();
            result.completeExceptionally(new CoreException(Activator.createErrorStatus(
                MessageFormat.format(Messages.CodeActions_Command_not_available, label), null)));
        }
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
