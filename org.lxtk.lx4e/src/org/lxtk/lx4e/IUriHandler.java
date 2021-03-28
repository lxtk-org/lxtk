/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
package org.lxtk.lx4e;

import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.lsp4j.CreateFileOptions;
import org.eclipse.lsp4j.DeleteFileOptions;
import org.eclipse.lsp4j.RenameFileOptions;
import org.eclipse.ltk.core.refactoring.Change;

/**
 * Provides information about resources denoted by URIs.
 *
 * @see UriHandlers
 */
public interface IUriHandler
{
    /**
     * Returns an element corresponding to the given URI.
     *
     * @param uri not <code>null</code>
     * @return the corresponding element, or <code>null</code> if none
     */
    Object getCorrespondingElement(URI uri);

    /**
     * Checks whether the given URI corresponds to an existing resource.
     *
     * @param uri not <code>null</code>
     * @return <ul><li><code>true</code> if the corresponding resource exists;</li>
     *  <li><code>false</code> if the corresponding resource does not exist;</li>
     *  <li><code>null</code> if there is no corresponding resource</li></ul>
     */
    Boolean exists(URI uri);

    /**
     * Returns a buffer that contains text contents of the resource denoted by
     * the given URI.
     *
     * @param uri not <code>null</code>
     * @return the corresponding buffer, or <code>null</code> if none.
     *  It is the client responsibility to {@link IBuffer#release() release}
     *  the returned buffer after it is no longer needed
     * @throws CoreException if an exception occurs while accessing the contents
     *  of the corresponding resource
     */
    IBuffer getBuffer(URI uri) throws CoreException;

    /**
     * Returns a string that identifies the resource denoted by the given URI
     * in a form suitable for displaying to the user, e.g., in message dialogs.
     *
     * @param uri not <code>null</code>
     * @return the corresponding string, or <code>null</code> if none
     */
    String toDisplayString(URI uri);

    /**
     * Returns a change for creating the resource denoted by the given URI.
     *
     * @param uri not <code>null</code>
     * @param options may be <code>null</code>
     * @return the create file change, or <code>null</code> if none
     * @throws CoreException if an exception occurs while creating the change
     */
    Change getCreateFileChange(URI uri, CreateFileOptions options) throws CoreException;

    /**
     * Returns a change for deleting the resource denoted by the given URI.
     *
     * @param uri not <code>null</code>
     * @param options may be <code>null</code>
     * @return the delete file change, or <code>null</code> if none
     * @throws CoreException if an exception occurs while creating the change
     */
    Change getDeleteFileChange(URI uri, DeleteFileOptions options) throws CoreException;

    /**
     * Returns a change for renaming the resource denoted by the given URI.
     *
     * @param uri not <code>null</code>
     * @param newUri not <code>null</code>
     * @param options may be <code>null</code>
     * @return the rename file change, or <code>null</code> if none
     * @throws CoreException if an exception occurs while creating the change
     */
    Change getRenameFileChange(URI uri, URI newUri, RenameFileOptions options) throws CoreException;
}
