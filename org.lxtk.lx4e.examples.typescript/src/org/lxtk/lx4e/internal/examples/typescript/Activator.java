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
package org.lxtk.lx4e.internal.examples.typescript;

import static org.lxtk.util.connect.Connectable.ConnectionState.DISCONNECTED;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.lxtk.TextDocument;
import org.lxtk.lx4e.EclipseTextDocument;
import org.lxtk.lx4e.examples.typescript.TypeScriptCore;
import org.lxtk.util.Disposable;
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
    public static final String PLUGIN_ID = "org.lxtk.lx4e.examples.typescript"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    private Runnable stopRunnable;
    private TypeScriptSourceFileDocumentProvider documentProvider;
    private Map<IProject, Disposable> connectedProjects;

    public TypeScriptSourceFileDocumentProvider getDocumentProvider()
    {
        return documentProvider;
    }

    public synchronized void connect(IProject project)
    {
        if (project == null || connectedProjects == null
            || connectedProjects.containsKey(project))
            return;
        SafeRun.run(rollback ->
        {
            TypeScriptLanguageClient languageClient =
                new TypeScriptLanguageClient(project);
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

            languageClient.onDidChangeConnectionState().subscribe(
                new Consumer<Connectable>()
                {
                    boolean shutUp;

                    @Override
                    public void accept(Connectable c)
                    {
                        String errorMessage = languageClient.getErrorMessage();
                        if (errorMessage != null)
                        {
                            getWorkbench().getDisplay().asyncExec(() ->
                            {
                                if (!shutUp)
                                {
                                    shutUp = true;

                                    Shell shell = null;
                                    IWorkbenchWindow window =
                                        getWorkbench().getActiveWorkbenchWindow();
                                    if (window != null)
                                        shell = window.getShell();
                                    MessageDialog.openError(shell,
                                        "TypeScript Language Client",
                                        MessageFormat.format(
                                            "Unable to connect project ''{0}'' to TypeScript language server. Dependent language services will be disabled. See Error Log for details",
                                            project.getName()));
                                }
                            });
                        }
                    }
                });

            rollback.setLogger(e -> logError(e));
            connectedProjects.put(project, rollback::run);
        });
    }

    public synchronized void disconnect(IProject project)
    {
        if (connectedProjects == null)
            return;
        Disposable disposable = connectedProjects.remove(project);
        if (disposable != null)
            disposable.dispose();
    }

    private synchronized void disconnectAll()
    {
        Disposable.disposeAll(connectedProjects.values());
        connectedProjects = null;
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;

        SafeRun.run(rollback ->
        {
            connectedProjects = new HashMap<>();
            rollback.add(this::disconnectAll);

            rollback.add(
                TypeScriptCore.WORKSPACE.onDidAddTextDocument().subscribe(
                    document -> connect(getProject(document)))::dispose);

            ModelManager.INSTANCE.startup();
            rollback.add(() -> ModelManager.INSTANCE.shutdown());

            documentProvider = new TypeScriptSourceFileDocumentProvider();

            rollback.setLogger(e -> logError(e));
            stopRunnable = rollback;
        });
    }

    private static IProject getProject(TextDocument document)
    {
        if (document instanceof EclipseTextDocument)
        {
            IFile file = ((EclipseTextDocument)document).getCorrespondingFile();
            if (file != null)
                return file.getProject();
        }
        return null;
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
