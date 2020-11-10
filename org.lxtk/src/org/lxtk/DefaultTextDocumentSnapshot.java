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
 * Default implementation of the {@link TextDocumentSnapshot} interface.
 */
public class DefaultTextDocumentSnapshot
    implements TextDocumentSnapshot
{
    private final TextDocument document;
    private final int version;
    private final String text;

    /**
     * Constructor.
     *
     * @param document the snapshot's document (not <code>null</code>)
     * @param version the snapshot's version (non-negative)
     * @param text the snapshot's text (not <code>null</code>)
     */
    public DefaultTextDocumentSnapshot(TextDocument document, int version, String text)
    {
        this.document = Objects.requireNonNull(document);
        if (version < 0)
            throw new IllegalArgumentException();
        this.version = version;
        this.text = Objects.requireNonNull(text);
    }

    @Override
    public final TextDocument getDocument()
    {
        return document;
    }

    @Override
    public final int getVersion()
    {
        return version;
    }

    @Override
    public final String getText()
    {
        return text;
    }
}
