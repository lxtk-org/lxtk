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

import java.util.Set;

/**
 * Defines the standard variable names.
 */
public class StandardVariableNames
{
    /** The currently selected text. */
    public static final String TM_SELECTED_TEXT = "TM_SELECTED_TEXT"; //$NON-NLS-1$
    /** The contents of the current line. */
    public static final String TM_CURRENT_LINE = "TM_CURRENT_LINE"; //$NON-NLS-1$
    /** The contents of the word under cursor. */
    public static final String TM_CURRENT_WORD = "TM_CURRENT_WORD"; //$NON-NLS-1$
    /** The zero-based line number. */
    public static final String TM_LINE_INDEX = "TM_LINE_INDEX"; //$NON-NLS-1$
    /** The one-based line number. */
    public static final String TM_LINE_NUMBER = "TM_LINE_NUMBER"; //$NON-NLS-1$
    /** The filename of the current document. */
    public static final String TM_FILENAME = "TM_FILENAME"; //$NON-NLS-1$
    /** The filename of the current document without its extensions. */
    public static final String TM_FILENAME_BASE = "TM_FILENAME_BASE"; //$NON-NLS-1$
    /** The directory of the current document. */
    public static final String TM_DIRECTORY = "TM_DIRECTORY"; //$NON-NLS-1$
    /** The full file path of the current document. */
    public static final String TM_FILEPATH = "TM_FILEPATH"; //$NON-NLS-1$

    private static final Set<String> ALL =
        Set.of(TM_SELECTED_TEXT, TM_CURRENT_LINE, TM_CURRENT_WORD, TM_LINE_INDEX, TM_LINE_NUMBER,
            TM_FILENAME, TM_FILENAME_BASE, TM_DIRECTORY, TM_FILEPATH);

    /**
     * Returns whether the given name is a standard variable name.
     *
     * @param name may be <code>null</code>
     * @return <code>true</code> if the name is a standard variable name,
     *  and <code>false</code> otherwise
     */
    public static boolean contains(String name)
    {
        return ALL.contains(name);
    }

    private StandardVariableNames()
    {
    }
}
