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
 * Options for creating a resource.
 */
public class CreateResourceOptions
{
    private final boolean overwrite;
    private final boolean ignoreIfExists;

    /**
     * Constructor.
     *
     * @param overwrite overwrite the resource if it already exists; wins over 'ignoreIfExists'
     * @param ignoreIfExists ignore the operation if the resource already exists
     */
    public CreateResourceOptions(boolean overwrite, boolean ignoreIfExists)
    {
        this.overwrite = overwrite;
        this.ignoreIfExists = ignoreIfExists;
    }

    /**
     * Indicates whether to overwrite the resource if it already exists.
     * Wins over {@link #isIgnoreIfExists()}.
     *
     * @return whether to overwrite the resource if it already exists
     */
    public boolean isOverwrite()
    {
        return overwrite;
    }

    /**
     * Indicates whether to ignore the operation if the resource already exists.
     *
     * @return whether to ignore the operation if the resource already exists
     */
    public boolean isIgnoreIfExists()
    {
        return ignoreIfExists;
    }
}
