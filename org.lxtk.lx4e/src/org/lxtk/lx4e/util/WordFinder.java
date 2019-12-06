/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

/**
 * TODO JavaDoc
 */
public class WordFinder
{
    /**
     * TODO JavaDoc
     *
     */
    protected WordFinder()
    {
    }

    /**
     * TODO JavaDoc
     *
     * @param document not <code>null</code>
     * @param offset 0-based
     * @return the corresponding word region, or <code>null</code> if none
     */
    public IRegion findWord(IDocument document, int offset)
    {
        int start = -2;
        int end = -1;

        try
        {
            int pos = offset;
            char c;

            while (pos >= 0 && pos < document.getLength())
            {
                c = document.getChar(pos);
                if (!isIdentifierPart(c))
                    break;
                --pos;
            }

            start = pos;

            pos = offset;
            int length = document.getLength();

            while (pos < length)
            {
                c = document.getChar(pos);
                if (!isIdentifierPart(c))
                    break;
                ++pos;
            }

            end = pos;
        }
        catch (BadLocationException x)
        {
        }

        if (start >= -1 && end > -1)
        {
            if (start == offset && end == offset)
                return new Region(offset, 0);
            else if (start == offset)
                return new Region(start, end - start);
            else
                return new Region(start + 1, end - start - 1);
        }

        return null;
    }

    protected boolean isIdentifierPart(char ch)
    {
        return Character.isUnicodeIdentifierPart(ch);
    }
}
