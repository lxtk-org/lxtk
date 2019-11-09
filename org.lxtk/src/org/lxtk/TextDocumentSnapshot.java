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
package org.lxtk;

import java.util.Objects;

/**
 * TODO JavaDoc
 */
public final class TextDocumentSnapshot
{
    private final TextDocument document;
    private final int version;
    private final String text;

    /**
     * TODO JavaDoc
     *
     * @param document not <code>null</code>
     * @param version not negative
     * @param text not <code>null</code>
     */
    public TextDocumentSnapshot(TextDocument document, int version, String text)
    {
        this.document = Objects.requireNonNull(document);
        if (version < 0)
            throw new IllegalArgumentException();
        this.version = version;
        this.text = Objects.requireNonNull(text);
    }

    /**
     * TODO JavaDoc
     *
     * @return the snapshot's document (never <code>null</code>)
     */
    public TextDocument getDocument()
    {
        return document;
    }

    /**
     * TODO JavaDoc
     *
     * @return the snapshot's version (non-negative)
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * TODO JavaDoc
     *
     * @return the snapshot's text (never <code>null</code>)
     */
    public String getText()
    {
        return text;
    }
}
