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
package org.lxtk.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * TODO JavaDoc
 */
public interface Log
{
    /**
     * TODO JavaDoc
     *
     * @param message not <code>null</code>
     */
    void error(String message);

    /**
     * TODO JavaDoc
     *
     * @param message not <code>null</code>
     * @param thrown not <code>null</code>
     */
    default void error(String message, Throwable thrown)
    {
        error(format(message, thrown));
    }

    /**
     * TODO JavaDoc
     *
     * @param message not <code>null</code>
     */
    void warning(String message);

    /**
     * TODO JavaDoc
     *
     * @param message not <code>null</code>
     * @param thrown not <code>null</code>
     */
    default void warning(String message, Throwable thrown)
    {
        warning(format(message, thrown));
    }

    /**
     * TODO JavaDoc
     *
     * @param message not <code>null</code>
     */
    void info(String message);

    /**
     * TODO JavaDoc
     *
     * @param message not <code>null</code>
     * @param thrown not <code>null</code>
     */
    default void info(String message, Throwable thrown)
    {
        info(format(message, thrown));
    }

    /**
     * TODO JavaDoc
     *
     * @param message not <code>null</code>
     * @param thrown not <code>null</code>
     * @return a formatted message (never <code>null</code>)
     */
    static String format(String message, Throwable thrown)
    {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw))
        {
            pw.println(message);
            thrown.printStackTrace(pw);
        }
        return sw.toString();
    }
}
