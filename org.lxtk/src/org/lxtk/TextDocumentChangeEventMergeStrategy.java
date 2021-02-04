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
package org.lxtk;

/**
 * Represents a strategy for merging text document change events.
 */
public interface TextDocumentChangeEventMergeStrategy
{
    /**
     * Returns a new instance of the builder for merging text document change events
     * based on the given text.
     *
     * @param base text before changes (not <code>null</code>)
     * @return the created builder object (never <code>null</code>)
     */
    TextDocumentChangeEventMergeBuilder startMerging(String base);
}
