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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.lxtk.client.AbstractLanguageClient;
import org.lxtk.client.Feature;
import org.lxtk.util.Log;

/**
 * TODO JavaDoc
 *
 * @param <S> server interface type
 */
public class EclipseLanguageClient<S extends LanguageServer>
    extends AbstractLanguageClient<S>
{
    /**
     * TODO JavaDoc
     *
     * @param log not <code>null</code>
     * @param diagnosticRequestor not <code>null</code>
     * @param features not <code>null</code>
     */
    public EclipseLanguageClient(Log log,
        BiConsumer<URI, Collection<Diagnostic>> diagnosticRequestor,
        Collection<Feature<? super S>> features)
    {
        super(log, diagnosticRequestor, features);
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
