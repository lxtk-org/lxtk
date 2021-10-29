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
package org.lxtk.lx4e.refactoring;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.lxtk.FileCreate;
import org.lxtk.FileDelete;
import org.lxtk.FileRename;

/**
 * Support for participants in file operations.
 */
public interface IFileOperationParticipantSupport
{
    /**
     * Returns a {@link Change} object that contains the workspace modifications to be executed
     * before the given files are created.
     *
     * @param files not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the change representing the workspace modifications to be executed
     *  before the given files are created, or <code>null</code> if no changes are made
     * @throws CoreException if an error occurred while creating the change
     * @throws OperationCanceledException if the change creation got canceled
     */
    Change computePreCreateChange(List<FileCreate> files, IProgressMonitor monitor)
        throws CoreException;

    /**
     * Returns a {@link Change} object that contains the workspace modifications to be executed
     * before the given files are deleted.
     *
     * @param files not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the change representing the workspace modifications to be executed
     *  before the given files are deleted, or <code>null</code> if no changes are made
     * @throws CoreException if an error occurred while creating the change
     * @throws OperationCanceledException if the change creation got canceled
     */
    Change computePreDeleteChange(List<FileDelete> files, IProgressMonitor monitor)
        throws CoreException;

    /**
     * Returns a {@link Change} object that contains the workspace modifications to be executed
     * before the given files are renamed.
     *
     * @param files not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the change representing the workspace modifications to be executed
     *  before the given files are renamed, or <code>null</code> if no changes are made
     * @throws CoreException if an error occurred while creating the change
     * @throws OperationCanceledException if the change creation got canceled
     */
    Change computePreRenameChange(List<FileRename> files, IProgressMonitor monitor)
        throws CoreException;

    /**
     * Returns a {@link Change} object that contains the workspace modifications to be executed
     * after the given files are created.
     *
     * @param files not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the change representing the workspace modifications to be executed
     *  after the given files are created, or <code>null</code> if no changes are made
     * @throws CoreException if an error occurred while creating the change
     * @throws OperationCanceledException if the change creation got canceled
     */
    Change computePostCreateChange(List<FileCreate> files, IProgressMonitor monitor)
        throws CoreException;

    /**
     * Returns a {@link Change} object that contains the workspace modifications to be executed
     * after the given files are deleted.
     *
     * @param files not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the change representing the workspace modifications to be executed
     *  after the given files are deleted, or <code>null</code> if no changes are made
     * @throws CoreException if an error occurred while creating the change
     * @throws OperationCanceledException if the change creation got canceled
     */
    Change computePostDeleteChange(List<FileDelete> files, IProgressMonitor monitor)
        throws CoreException;

    /**
     * Returns a {@link Change} object that contains the workspace modifications to be executed
     * after the given files are renamed.
     *
     * @param files not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the change representing the workspace modifications to be executed
     *  after the given files are renamed, or <code>null</code> if no changes are made
     * @throws CoreException if an error occurred while creating the change
     * @throws OperationCanceledException if the change creation got canceled
     */
    Change computePostRenameChange(List<FileRename> files, IProgressMonitor monitor)
        throws CoreException;
}
