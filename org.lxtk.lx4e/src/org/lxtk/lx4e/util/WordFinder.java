/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Vladimir Piskarev (1C) - adaptation
 *     (adapted from org.eclipse.jdt.internal.ui.text.JavaWordFinder
 *               and org.eclipse.jface.text.DefaultTextHover.findWord)
 *******************************************************************************/
package org.lxtk.lx4e.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.lxtk.lx4e.internal.Activator;

/**
 * Finds the <i>word</i> at a given document offset.
 * <p>
 * This class provides a default behavior that searches for <i>Unicode identifiers</i>.
 * Clients can use the {@link DefaultWordFinder#INSTANCE default} instance of the
 * word finder or may subclass this class if they need to specialize the default
 * behavior.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class WordFinder
{
    /**
     * Constructor.
     */
    protected WordFinder()
    {
    }

    /**
     * Returns the region of the word enclosing the given document offset.
     *
     * @param document not <code>null</code>
     * @param offset 0-based
     * @return the corresponding word region, or <code>null</code> if none
     */
    public IRegion findWord(IDocument document, int offset)
    {
        return findWord(document, offset, false);
    }

    /**
     * Returns the region of the word enclosing the given document offset,
     * using the given search mode.
     *
     * @param document not <code>null</code>
     * @param offset 0-based
     * @param strict the search mode:
     *  <ul>
     *  <li>in strict mode, the offset must fall strictly within the word region;</li>
     *  <li>in non-strict mode, the offset may immediately follow the word region,
     *  i.e., the end offset of the word region may equal <code>offset - 1</code>.</li>
     *  </ul>
     * @return the corresponding word region, or <code>null</code> if none
     */
    public IRegion findWord(IDocument document, int offset, boolean strict)
    {
        int start;
        int end;

        try
        {
            if (!strict && offset > 0 && !isWordPart(document.getChar(offset)))
                --offset;

            int pos = offset;
            do
            {
                if (!isWordPart(document.getChar(pos)))
                    break;
                --pos;
            }
            while (pos >= 0);

            start = pos;

            int length = document.getLength();
            pos = offset;
            do
            {
                if (!isWordPart(document.getChar(pos)))
                    break;
                ++pos;
            }
            while (pos < length);

            end = pos;
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }

        if (start == offset && end == offset)
            return null;

        if (start == offset)
            return new Region(start, end - start);

        return new Region(start + 1, end - start - 1);
    }

    /**
     * Determines whether the given character may be part of a word.
     *
     * @param ch a character
     * @return <code>true</code> if the given character may be part of a word,
     *  and <code>false</code> otherwise
     */
    protected boolean isWordPart(char ch)
    {
        return Character.isUnicodeIdentifierPart(ch);
    }
}
