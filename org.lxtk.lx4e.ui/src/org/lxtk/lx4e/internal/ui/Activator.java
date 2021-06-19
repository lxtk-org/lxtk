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
package org.lxtk.lx4e.internal.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;
import org.eclipse.swt.graphics.Image;
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

    private static final String T_DLCL = "/dlcl16/"; //$NON-NLS-1$
    private static final String T_ELCL = "/elcl16/"; //$NON-NLS-1$

    public static final String IMG_DLCL_CONFIGURE_ANNOTATIONS =
        PLUGIN_ID + T_DLCL + "configure_annotations.png"; //$NON-NLS-1$
    public static final String IMG_ELCL_CONFIGURE_ANNOTATIONS =
        PLUGIN_ID + T_ELCL + "configure_annotations.png"; //$NON-NLS-1$

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

    public static Throwable unwrap(Throwable e)
    {
        if (e instanceof CompletionException || e instanceof ExecutionException
            || e instanceof InvocationTargetException)
        {
            Throwable cause = e.getCause();
            if (cause != null)
                return unwrap(cause);
        }
        return e;
    }

    public static boolean isCancellation(Throwable e)
    {
        return e instanceof OperationCanceledException || e instanceof CancellationException
            || e instanceof InterruptedException;
    }

    public static Image getImage(String symbolicName)
    {
        return plugin.getImageRegistry().get(symbolicName);
    }

    public static ImageDescriptor getImageDescriptor(String symbolicName)
    {
        return plugin.getImageRegistry().getDescriptor(symbolicName);
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry registry)
    {
        LSPImages.initialize(registry);

        registry.put(IMG_DLCL_CONFIGURE_ANNOTATIONS,
            imageDescriptorFromSymbolicName(IMG_DLCL_CONFIGURE_ANNOTATIONS));
        registry.put(IMG_ELCL_CONFIGURE_ANNOTATIONS,
            imageDescriptorFromSymbolicName(IMG_ELCL_CONFIGURE_ANNOTATIONS));
    }

    private static ImageDescriptor imageDescriptorFromSymbolicName(String symbolicName)
    {
        String path = "$nl$/icons/full" + symbolicName.substring(PLUGIN_ID.length()); //$NON-NLS-1$
        return ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, path).orElse(null);
    }
}
