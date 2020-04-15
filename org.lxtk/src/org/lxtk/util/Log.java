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
package org.lxtk.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Interface for message logging.
 */
public interface Log
{
    /**
     * Logs an error message.
     *
     * @param message not <code>null</code>
     */
    void error(String message);

    /**
     * Logs an error message, with associated <code>Throwable</code>.
     *
     * @param message not <code>null</code>
     * @param thrown the <code>Throwable</code> associated with the message
     *  (not <code>null</code>)
     */
    default void error(String message, Throwable thrown)
    {
        error(format(message, thrown));
    }

    /**
     * Logs a warning message.
     *
     * @param message not <code>null</code>
     */
    void warning(String message);

    /**
     * Logs a warning message, with associated <code>Throwable</code>.
     *
     * @param message not <code>null</code>
     * @param thrown the <code>Throwable</code> associated with the message
     *  (not <code>null</code>)
     */
    default void warning(String message, Throwable thrown)
    {
        warning(format(message, thrown));
    }

    /**
     * Logs an info message.
     *
     * @param message not <code>null</code>
     */
    void info(String message);

    /**
     * Logs an info message, with associated <code>Throwable</code>.
     *
     * @param message not <code>null</code>
     * @param thrown the <code>Throwable</code> associated with the message
     *  (not <code>null</code>)
     */
    default void info(String message, Throwable thrown)
    {
        info(format(message, thrown));
    }

    /**
     * Formats a message, with associated <code>Throwable</code>.
     *
     * @param message not <code>null</code>
     * @param thrown the <code>Throwable</code> associated with the message
     *  (not <code>null</code>)
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
