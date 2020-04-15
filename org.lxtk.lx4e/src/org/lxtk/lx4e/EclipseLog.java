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
package org.lxtk.lx4e;

import java.text.MessageFormat;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.lxtk.util.Log;
import org.osgi.framework.Bundle;

/**
 * Implements a {@link Log} that delegates to the {@link ILog} of a given Eclipse bundle.
 */
public final class EclipseLog
    implements Log
{
    private final String pluginId;
    private final ILog log;
    private final String name;

    /**
     * Shortcut to <code>new EclipseLog(bundle, null)</code>.
     *
     * @param bundle a {@link Bundle} (not <code>null</code>)
     * @see #EclipseLog(Bundle, String)
     */
    public EclipseLog(Bundle bundle)
    {
        this(bundle, null);
    }

    /**
     * Constructs an {@link EclipseLog} for the given bundle.
     *
     * @param bundle a {@link Bundle} (not <code>null</code>)
     * @param name optional name for the log (may be <code>null</code>)
     */
    public EclipseLog(Bundle bundle, String name)
    {
        this.pluginId = bundle.getSymbolicName();
        this.log = Platform.getLog(bundle);
        this.name = name;
    }

    @Override
    public void error(String message)
    {
        error(message, null);
    }

    @Override
    public void error(String message, Throwable thrown)
    {
        log.log(new Status(IStatus.ERROR, pluginId, format(message), thrown));
    }

    @Override
    public void warning(String message)
    {
        warning(message, null);
    }

    @Override
    public void warning(String message, Throwable thrown)
    {
        log.log(new Status(IStatus.WARNING, pluginId, format(message), thrown));
    }

    @Override
    public void info(String message)
    {
        info(message, null);
    }

    @Override
    public void info(String message, Throwable thrown)
    {
        log.log(new Status(IStatus.INFO, pluginId, format(message), thrown));
    }

    private String format(String message)
    {
        if (name == null)
            return message;
        return MessageFormat.format("[{0}] {1}", name, message); //$NON-NLS-1$
    }
}
