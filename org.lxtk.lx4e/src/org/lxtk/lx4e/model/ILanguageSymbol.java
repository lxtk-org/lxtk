/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.model.ISourceConstruct;
import org.eclipse.handly.util.Property;
import org.eclipse.lsp4j.SymbolKind;

/**
 * Common interface for language symbols.
 */
public interface ILanguageSymbol
    extends ILanguageSourceElement, ISourceConstruct
{
    /**
     * An optional detail for the symbol.
     * @see #getSourceElementInfo(IProgressMonitor)
     */
    Property<String> DETAIL = Property.get("detail", String.class); //$NON-NLS-1$
    /**
     * An optional deprecation indicator for the symbol.
     * @see #getSourceElementInfo(IProgressMonitor)
     */
    Property<Boolean> DEPRECATED = Property.get("deprecated", Boolean.class); //$NON-NLS-1$

    /**
     * Returns the kind of this symbol. This is a handle-only method.
     *
     * @return the kind of this symbol (never <code>null</code>)
     */
    SymbolKind getKind();

    /**
     * Returns the source file in which this symbol is declared,
     * or <code>null</code> if none. This is a handle-only method.
     *
     * @return the source file in which this symbol is declared,
     *  or <code>null</code> if none
     */
    ILanguageSourceFile getSourceFile();

    /**
     * Returns the symbol in which this symbol is declared, or <code>null</code>
     * if none (e.g., a top-level symbol). This is a handle-only method.
     *
     * @return the symbol in which this symbol is declared,
     *  or <code>null</code> if none
     */
    ILanguageSymbol getDeclaringSymbol();

    /**
     * Returns the member symbol declared in this symbol with the given simple
     * name and the given kind. This is a handle-only method. The returned
     * symbol may or may not exist.
     *
     * @param name the simple name of the requested symbol (not <code>null</code>)
     * @param kind the kind of the requested symbol (not <code>null</code>)
     * @return a handle onto the corresponding symbol (never <code>null</code>).
     *  The symbol may or may not exist.
     */
    ILanguageSymbol getSymbol(String name, SymbolKind kind);

    /**
     * Returns the immediate member symbols declared by this symbol.
     *
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the immediate member symbols declared by this symbol (never
     *  <code>null</code>). Clients <b>must not</b> modify the returned array.
     * @throws CoreException if this symbol does not exist or if an
     *  exception occurs while accessing its corresponding resource
     */
    ILanguageSymbol[] getSymbols(IProgressMonitor monitor) throws CoreException;
}
