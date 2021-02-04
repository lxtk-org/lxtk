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
 * Represents a builder object for merging text document change events.
 * The events to be merged must have been already applied to the same document
 * in the sequence given.
 * <p>
 * Implementations of this interface are not expected to be thread-safe.
 * </p>
 */
public interface TextDocumentChangeEventMergeBuilder
{
    /**
     * Merges the given text document change event.
     *
     * @param event not <code>null</code>
     * @return this builder
     * @throws IllegalArgumentException if merging cannot be performed for the given argument
     */
    TextDocumentChangeEventMergeBuilder merge(TextDocumentChangeEvent event);

    /**
     * Returns the current result of the merge.
     * <p>
     * Note: a new object may be returned each time this method is called.
     * </p>
     *
     * @return the current merge result, or <code>null</code> if none yet
     */
    TextDocumentChangeEvent getResult();

    /**
     * Checks whether there is a merge result currently.
     *
     * @return <code>true</code> if there is currently a merge result,
     *  and <code>false</code> if there is no merge result yet
     */
    boolean hasResult();
}
