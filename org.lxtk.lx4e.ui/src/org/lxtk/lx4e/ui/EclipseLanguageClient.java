/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
package org.lxtk.lx4e.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceEditCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.Feature;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.refactoring.WorkspaceEditRefactoring;
import org.lxtk.util.Log;

/**
 * TODO JavaDoc
 *
 * @param <S> server interface type
 */
public class EclipseLanguageClient<S extends LanguageServer>
    extends AbstractLanguageClient<S>
{
    private final WorkspaceEditChangeFactory workspaceEditChangeFactory;

    /**
     * TODO JavaDoc
     *
     * @param log not <code>null</code>
     * @param diagnosticRequestor not <code>null</code>
     * @param workspaceEditChangeFactory not <code>null</code>
     * @param features not <code>null</code>
     */
    public EclipseLanguageClient(Log log,
        BiConsumer<URI, Collection<Diagnostic>> diagnosticRequestor,
        WorkspaceEditChangeFactory workspaceEditChangeFactory,
        Collection<Feature<? super S>> features)
    {
        super(log, diagnosticRequestor, features);
        this.workspaceEditChangeFactory = Objects.requireNonNull(
            workspaceEditChangeFactory);
    }

    @Override
    public void fillClientCapabilities(ClientCapabilities capabilities)
    {
        WorkspaceEditCapabilities workspaceEdit =
            new WorkspaceEditCapabilities();
        workspaceEdit.setDocumentChanges(true);
        // TODO Support resource operations
//        workspaceEdit.setResourceOperations(Arrays.asList(
//            ResourceOperationKind.Create, ResourceOperationKind.Delete,
//            ResourceOperationKind.Rename));

        WorkspaceClientCapabilities workspace =
            new WorkspaceClientCapabilities();
        workspace.setApplyEdit(true);
        workspace.setWorkspaceEdit(workspaceEdit);

        capabilities.setWorkspace(workspace);
        super.fillClientCapabilities(capabilities);
    }

    @Override
    public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(
        ApplyWorkspaceEditParams params)
    {
        String label = getEditLabel(params);

        WorkspaceEditRefactoring refactoring = new WorkspaceEditRefactoring(
            label, workspaceEditChangeFactory);
        refactoring.setWorkspaceEdit(params.getEdit());

        PerformRefactoringOperation operation = new PerformRefactoringOperation(
            refactoring, CheckConditionsOperation.ALL_CONDITIONS);

        CompletableFuture<ApplyWorkspaceEditResponse> future =
            new CompletableFuture<>();
        WorkspaceJob job = new WorkspaceJob(label)
        {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor)
                throws CoreException
            {
                try
                {
                    operation.run(monitor);

                    RefactoringStatus status = operation.getConditionStatus();
                    if (status != null && !status.hasFatalError())
                        status = operation.getValidationStatus();

                    ApplyWorkspaceEditResponse response =
                        new ApplyWorkspaceEditResponse();
                    response.setApplied(status != null
                        && !status.hasFatalError());
                    // TODO Set ApplyWorkspaceEditResponse.failureReason when it becomes available in LSP4J

                    future.complete(response);
                    return Status.OK_STATUS;
                }
                catch (OperationCanceledException e)
                {
                    future.cancel(false);
                    throw e;
                }
                catch (Throwable e)
                {
                    future.completeExceptionally(e);
                    throw e;
                }
            }
        };
        job.setRule(ResourcesPlugin.getWorkspace().getRoot());
        job.schedule();
        return future;
    }

    /**
     * TODO JavaDoc
     *
     * @param params never <code>null</code>
     * @return the corresponding label (not <code>null</code>)
     */
    protected String getEditLabel(ApplyWorkspaceEditParams params)
    {
        String label = params.getLabel();
        if (label == null || label.isEmpty())
            label = Messages.EclipseLanguageClient_Edit_label;
        return label;
    }

    @Override
    public void telemetryEvent(Object object)
    {
    }

    @Override
    public void showMessage(MessageParams params)
    {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() ->
        {
            MessageDialog dialog = new MessageDialog(getShell(),
                getMessageTitle(params), null, params.getMessage(),
                getDialogImageType(params), 0, IDialogConstants.OK_LABEL);
            dialog.open();
        });
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(
        ShowMessageRequestParams params)
    {
        CompletableFuture<MessageActionItem> future = new CompletableFuture<>();
        PlatformUI.getWorkbench().getDisplay().asyncExec(() ->
        {
            List<MessageActionItem> actions = params.getActions();
            MessageDialog dialog = new MessageDialog(getShell(),
                getMessageTitle(params), null, params.getMessage(),
                getDialogImageType(params), 0, getDialogButtonLabels(actions));
            int index = dialog.open();
            if (index == SWT.DEFAULT || actions == null || actions.isEmpty())
                future.complete(null);
            else
                future.complete(actions.get(index));
        });
        return future;
    }

    /**
     * TODO JavaDoc
     *
     * @param params never <code>null</code>
     * @return the corresponding title (may be <code>null</code>)
     */
    protected String getMessageTitle(MessageParams params)
    {
        return Messages.EclipseLanguageClient_Message_title;
    }

    private static int getDialogImageType(MessageParams params)
    {
        switch (params.getType())
        {
        case Error:
            return MessageDialog.ERROR;
        case Warning:
            return MessageDialog.WARNING;
        case Info:
            return MessageDialog.INFORMATION;
        default:
            return MessageDialog.NONE;
        }
    }

    private static String[] getDialogButtonLabels(
        List<MessageActionItem> actions)
    {
        List<String> labels = new ArrayList<>();
        if (actions == null || actions.isEmpty())
            labels.add(IDialogConstants.OK_LABEL);
        else
            for (MessageActionItem action : actions)
                labels.add(action.getTitle());
        return labels.toArray(new String[labels.size()]);
    }

    private static Shell getShell()
    {
        IWorkbenchWindow window =
            PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            return null;
        return window.getShell();
    }
}
