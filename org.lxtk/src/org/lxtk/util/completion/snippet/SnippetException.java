/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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
package org.lxtk.util.completion.snippet;

/**
 * Thrown to indicate that a source describes an invalid snippet.
 */
public class SnippetException
    extends Exception
{
    private static final long serialVersionUID = 7838741528285768620L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public SnippetException(String message)
    {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public SnippetException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
