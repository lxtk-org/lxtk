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

import java.util.List;

/**
 * An event describing creation of one or more files.
 */
public class FileCreateEvent
{
    private final List<FileCreate> files;

    /**
     * Constructor.
     *
     * @param files a list of file creations (not <code>null</code>, not empty)
     */
    public FileCreateEvent(List<FileCreate> files)
    {
        if (files.isEmpty())
            throw new IllegalArgumentException();
        this.files = files;
    }

    /**
     * Returns the list of file creations.
     *
     * @return the list of file creations (never <code>null</code>, never empty).
     *  Clients <b>must not</b> modify the returned list.
     */
    public final List<FileCreate> getFiles()
    {
        return files;
    }
}
