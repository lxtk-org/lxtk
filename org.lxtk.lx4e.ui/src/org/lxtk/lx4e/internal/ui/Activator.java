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
package org.lxtk.lx4e.internal.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator
    extends AbstractUIPlugin
{
    /** The plug-in ID */
    public static final String PLUGIN_ID = "org.lxtk.lx4e.ui"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;
        RefactoringUI.class.getClass(); // ensure that Refactoring UI is started
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
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

    public static IStatus createWarningStatus(String msg, Throwable e)
    {
        return new Status(IStatus.WARNING, PLUGIN_ID, 0, msg, e);
    }

    public static void logError(String msg, Throwable e)
    {
        plugin.getLog().log(createErrorStatus(msg, e));
    }

    public static void logWarning(String msg, Throwable e)
    {
        plugin.getLog().log(createWarningStatus(msg, e));
    }

    public static void logError(Throwable e)
    {
        logError(e.getMessage(), e);
    }

    public static void logWarning(Throwable e)
    {
        logWarning(e.getMessage(), e);
    }

    public static boolean isCancellation(Throwable e)
    {
        if (e instanceof CompletionException || e instanceof ExecutionException
            || e instanceof InvocationTargetException)
        {
            return isCancellation(e.getCause());
        }
        return e instanceof OperationCanceledException || e instanceof CancellationException;
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry registry)
    {
        LSPImages.initialize(registry);
    }
}
