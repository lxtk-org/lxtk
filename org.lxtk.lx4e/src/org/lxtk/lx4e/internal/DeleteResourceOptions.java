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
package org.lxtk.lx4e.internal;

/**
 * Options for deleting a resource.
 */
public class DeleteResourceOptions
{
    private final boolean recursive;
    private final boolean ignoreIfNotExists;

    /**
     * Constructor.
     *
     * @param recursive delete the content recursively if a folder is denoted
     * @param ignoreIfNotExists ignore the operation if the resource does not exist
     */
    public DeleteResourceOptions(boolean recursive, boolean ignoreIfNotExists)
    {
        this.recursive = recursive;
        this.ignoreIfNotExists = ignoreIfNotExists;
    }

    /**
     * Indicates whether to delete the content recursively if a folder is denoted.
     *
     * @return whether to delete the content recursively if a folder is denoted
     */
    public boolean isRecursive()
    {
        return recursive;
    }

    /**
     * Indicates whether to ignore the operation if the resource does not exist.
     *
     * @return whether to ignore the operation if the resource does not exist
     */
    public boolean isIgnoreIfNotExists()
    {
        return ignoreIfNotExists;
    }
}
