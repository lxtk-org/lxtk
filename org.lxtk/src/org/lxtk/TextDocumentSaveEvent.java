/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk;

import java.util.Objects;

/**
 * An event describing a save of a {@link TextDocument}.
 */
public class TextDocumentSaveEvent
{
    private final TextDocument document;
    private final String text;

    /**
     * Constructor.
     *
     * @param document the document that was saved (not <code>null</code>)
     * @param text the document content when saved
     */
    public TextDocumentSaveEvent(TextDocument document, String text)
    {
        this.document = Objects.requireNonNull(document);
        this.text = text;
    }

    /**
     * Returns the document that was saved.
     *
     * @return the document that was saved (never <code>null</code>)
     */
    public final TextDocument getDocument()
    {
        return document;
    }

    /**
     * Returns the document content when saved.
     *
     * @return the document content when saved
     */
    public final String getText()
    {
        return text;
    }
}
