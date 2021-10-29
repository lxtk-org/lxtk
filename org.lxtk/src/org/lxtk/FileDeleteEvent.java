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
 * An event describing deletion of one or more files.
 */
public class FileDeleteEvent
{
    private final List<FileDelete> files;

    /**
     * Constructor.
     *
     * @param files a list of file deletions (not <code>null</code>, not empty)
     */
    public FileDeleteEvent(List<FileDelete> files)
    {
        if (files.isEmpty())
            throw new IllegalArgumentException();
        this.files = files;
    }

    /**
     * Returns the list of file deletions.
     *
     * @return the list of file deletions (never <code>null</code>, never empty).
     *  Clients <b>must not</b> modify the returned list.
     */
    public final List<FileDelete> getFiles()
    {
        return files;
    }
}
