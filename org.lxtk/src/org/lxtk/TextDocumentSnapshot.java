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
package org.lxtk;

/**
 * Represents an immutable snapshot of a {@link TextDocument}.
 */
public interface TextDocumentSnapshot
{
    /**
     * Returns the snapshot's document.
     *
     * @return the snapshot's document (never <code>null</code>)
     */
    TextDocument getDocument();

    /**
     * Returns the snapshot's version.
     *
     * @return the snapshot's version (non-negative)
     */
    int getVersion();

    /**
     * Returns the snapshot's text.
     *
     * @return the snapshot's text (never <code>null</code>)
     */
    String getText();
}
