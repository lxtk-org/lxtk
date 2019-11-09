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
package org.lxtk.lx4e.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator
    extends Plugin
{
    public static final String PLUGIN_ID = "org.lxtk.lx4e"; //$NON-NLS-1$

    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        super.stop(context);
        plugin = null;
    }

    public static Activator getDefault()
    {
        return plugin;
    }

    public static IStatus createErrorStatus(String msg, Throwable e)
    {
        return new Status(IStatus.ERROR, PLUGIN_ID, msg, e);
    }

    public static void logError(String msg, Throwable e)
    {
        plugin.getLog().log(createErrorStatus(msg, e));
    }

    public static void logError(Throwable e)
    {
        logError(e.getMessage(), e);
    }

    public static CoreException toCoreException(Throwable e)
    {
        return new CoreException(createErrorStatus(e.getMessage(), e));
    }
}
