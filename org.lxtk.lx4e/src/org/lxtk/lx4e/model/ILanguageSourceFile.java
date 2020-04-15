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
package org.lxtk.lx4e.model;

import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.handly.model.ISourceFileExtension;
import org.eclipse.lsp4j.SymbolKind;

/**
 * Common interface for language source files.
 */
public interface ILanguageSourceFile
    extends ILanguageSourceElement, ISourceFileExtension
{
    /**
     * Returns the corresponding document URI (in the LSP sense) for this
     * source file. Returns <code>null</code> if the document URI cannot be
     * determined at the time of the call.
     * <p>
     * In general, the document URI may change over the lifetime of the
     * source file. However, the document URI of a working copy is never
     * <code>null</code> and does not change over the lifetime of the working copy.
     * </p>
     *
     * @return the corresponding document URI (may be <code>null</code>)
     */
    URI getDocumentUri();

    /**
     * Returns a string uniquely identifying the language corresponding to
     * this source file. This is a handle-only method.
     *
     * @return the corresponding language identifier (never <code>null</code>)
     */
    String getLanguageId();

    /**
     * If this source file is not already in working copy mode, switches it
     * into a working copy, associates it with a working copy buffer, and
     * acquires an independent ownership of the working copy (and, hence,
     * of the working copy buffer). Performs atomically.
     * <p>
     * In working copy mode, the source file's structure and properties
     * shall no longer correspond to the underlying resource contents
     * and must no longer be updated by a resource delta processor.
     * Instead, the source file's structure and properties can be explicitly
     * {@link #reconcile(IProgressMonitor) reconciled} with the current
     * contents of the working copy buffer.
     * </p>
     * <p>
     * If the source file was already in working copy mode, this method acquires
     * a new independent ownership of the working copy by incrementing an internal
     * counter.
     * </p>
     * <p>
     * Each call to this method that didn't throw an exception must ultimately
     * be followed by exactly one call to {@link #releaseWorkingCopy()}.
     * </p>
     *
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @throws CoreException if the working copy could not be created successfully
     * @throws OperationCanceledException if this method is canceled
     */
    void becomeWorkingCopy(IProgressMonitor monitor) throws CoreException;

    /**
     * Relinquishes an independent ownership of the working copy by decrementing
     * an internal counter. If there are no remaining owners of the working copy,
     * switches this source file from working copy mode back to its original mode
     * and releases the working copy buffer. Performs atomically.
     * <p>
     * Each independent ownership of the working copy must ultimately end
     * with exactly one call to this method. Clients that do not own the
     * working copy must not call this method.
     * </p>
     */
    void releaseWorkingCopy();

    /**
     * Returns the top-level symbol with the given name and the given kind.
     * This is a handle-only method. The symbol may or may not exist.
     *
     * @param name the name of the requested symbol (not <code>null</code>)
     * @param kind the kind of the requested symbol (not <code>null</code>)
     * @return a handle onto the corresponding symbol (never <code>null</code>).
     *  The symbol may or may not exist
     */
    ILanguageSymbol getSymbol(String name, SymbolKind kind);

    /**
     * Returns the top-level symbols of this source file.
     *
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the top-level symbols of this source file (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned array
     * @throws CoreException if this source file does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    ILanguageSymbol[] getSymbols(IProgressMonitor monitor) throws CoreException;
}
