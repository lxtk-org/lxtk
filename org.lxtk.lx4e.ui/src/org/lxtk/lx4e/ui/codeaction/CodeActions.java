/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
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
        WorkspaceEditChangeFactory workspaceEditChangeFactory, CommandService commandService)
    {
        CompletableFuture<?> future = new CompletableFuture<>();
        WorkspaceEdit edit = codeAction.getEdit();
        if (edit == null)
            future.complete(null);
        else
        {
            WorkspaceEditRefactoring refactoring =
                new WorkspaceEditRefactoring(label, workspaceEditChangeFactory);
            refactoring.setWorkspaceEdit(edit);

            PerformRefactoringOperation operation = new PerformRefactoringOperation(refactoring,
                CheckConditionsOperation.ALL_CONDITIONS);

            WorkspaceJob job = new WorkspaceJob(label)
            {
                @Override
                public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
                {
                    try
                    {
                        operation.run(monitor);
                        future.complete(null);
                    }
                    catch (Throwable e)
                    {
                        future.completeExceptionally(e);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.schedule();
        }
        future.thenCompose(
            x -> doExecute(codeAction.getCommand(), null, commandService)).exceptionally(e ->
            {
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

    private CodeActions()
    {
    }
}
