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

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an evaluated snippet.
 */
public final class Snippet
{
    private final String text;
    private final TabStop[] tabStops;

    /**
     * Parses the snippet described by the given source string and evaluates it in the given context.
     *
     * @param source not <code>null</code>
     * @param context not <code>null</code>
     * @return the evaluated snippet (never <code>null</code>)
     * @throws SnippetException if the snippet described by the source is invalid
     */
    public static Snippet parse(String source, SnippetContext context) throws SnippetException
    {
        SnippetParser parser = new SnippetParser(source, context);
        return parser.parse();
    }

    Snippet(String text, TabStop... tabStops)
    {
        this.text = Objects.requireNonNull(text);
        this.tabStops = Objects.requireNonNull(tabStops);
    }

    /**
     * Returns the text of the evaluated snippet.
     *
     * @return the snippet text (never <code>null</code>, may be empty)
     */
    public String getText()
    {
        return text;
    }

    /**
     * Returns the tab stops within the evaluated snippet. The tab stops are ordered according
     * to their ordinal number, ascending.
     *
     * @return the snippet tab stops (never <code>null</code>, may be empty)
     */
    public TabStop[] getTabStops()
    {
        return tabStops;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Snippet other = (Snippet)obj;
        if (!text.equals(other.text))
            return false;
        if (!Arrays.equals(tabStops, other.tabStops))
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + text.hashCode();
        result = prime * result + Arrays.hashCode(tabStops);
        return result;
    }

    @Override
    public String toString()
    {
        return "{text=" + text + ", tabStops=" + Arrays.toString(tabStops) + '}'; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
