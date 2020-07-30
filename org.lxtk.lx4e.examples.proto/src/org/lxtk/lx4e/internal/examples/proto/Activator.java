/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.internal.examples.proto;

import static org.lxtk.util.connect.Connectable.ConnectionState.DISCONNECTED;

import java.util.function.Consumer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.lxtk.util.SafeRun;
import org.lxtk.util.connect.Connectable;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator
    extends AbstractUIPlugin
{
    // The plug-in ID
    public static final String PLUGIN_ID = "org.lxtk.lx4e.examples.proto"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    private Runnable stopRunnable;
    private ProtoLanguageClient languageClient;
    private IDocumentProvider documentProvider;

    public IDocumentProvider getDocumentProvider()
    {
        return documentProvider;
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;

        SafeRun.run(rollback ->
        {
            languageClient = new ProtoLanguageClient();
            languageClient.connect();
            rollback.add(() ->
            {
                languageClient.dispose();
                for (int i = 0; i < 100; i++)
                {
                    if (languageClient.getConnectionState() == DISCONNECTED)
                        break;
                    try
                    {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            });

            languageClient.onDidChangeConnectionState().subscribe(new Consumer<Connectable>()
            {
                boolean shutUp;

                @Override
                public void accept(Connectable c)
                {
                    String errorMessage = languageClient.getErrorMessage();
                    if (errorMessage != null)
                    {
                        PlatformUI.getWorkbench().getDisplay().asyncExec(() ->
                        {
                            if (!shutUp)
                            {
                                shutUp = true;

                                Shell shell = null;
                                IWorkbenchWindow window =
                                    PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                                if (window != null)
                                    shell = window.getShell();
                                MessageDialog.openError(shell, "Proto Language Client",
                                    "Unable to connect to the Proto Language Server. The dependent language services will be disabled. See the Error Log for details");
                            }
                        });
                    }
                }
            });

            documentProvider = new ProtoDocumentProvider();

            rollback.setLogger(e -> logError(e));
            stopRunnable = rollback;
        });
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        try
        {
            if (stopRunnable != null)
                stopRunnable.run();
        }
        finally
        {
            plugin = null;
            super.stop(context);
        }
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault()
    {
        return plugin;
    }

    public static IStatus createErrorStatus(String msg, Throwable e)
    {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, e);
    }

    public static void logError(String msg, Throwable e)
    {
        plugin.getLog().log(createErrorStatus(msg, e));
    }

    public static void logError(Throwable e)
    {
        logError(e.getMessage(), e);
    }
}
