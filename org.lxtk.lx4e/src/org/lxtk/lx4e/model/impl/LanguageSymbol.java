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
package org.lxtk.lx4e.model.impl;

import static java.util.Objects.requireNonNull;
import static org.eclipse.handly.context.Contexts.EMPTY_CONTEXT;
import static org.eclipse.handly.model.Elements.findAncestorOfType;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.support.IModelManager;
import org.eclipse.handly.model.impl.support.ISourceConstructImplSupport;
import org.eclipse.lsp4j.SymbolKind;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.model.ILanguageSymbol;

/**
 * Default implementation of {@link ILanguageSymbol}.
 */
public class LanguageSymbol
    extends LanguageSourceElement
    implements ILanguageSymbol, ISourceConstructImplSupport
{
    private final SymbolKind kind;
    private int occurrenceCount = 1;

    /**
     * Constructs a handle for a symbol with the given parent element,
     * name, and kind.
     *
     * @param parent the parent of the symbol (not <code>null</code>)
     * @param name the name of the symbol (not <code>null</code>)
     * @param kind the kind of the symbol (not <code>null</code>)
     */
    public LanguageSymbol(LanguageElement parent, String name, SymbolKind kind)
    {
        super(requireNonNull(parent), requireNonNull(name));
        this.kind = requireNonNull(kind);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof LanguageSymbol))
            return false;
        return kind == ((LanguageSymbol)obj).kind && super.equals(obj);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + kind.hashCode();
        return result;
    }

    @Override
    public final SymbolKind getKind()
    {
        return kind;
    }

    @Override
    public final ILanguageSourceFile getSourceFile()
    {
        return findAncestorOfType(getParent_(), ILanguageSourceFile.class);
    }

    @Override
    public ILanguageSymbol getContainingSymbol()
    {
        IElement parent = getParent_();
        if (parent instanceof ILanguageSymbol)
            return (ILanguageSymbol)parent;
        return null;
    }

    @Override
    public ILanguageSymbol getSymbol(String name, SymbolKind kind)
    {
        return new LanguageSymbol(this, name, kind);
    }

    @Override
    public ILanguageSymbol[] getSymbols(IProgressMonitor monitor) throws CoreException
    {
        return (ILanguageSymbol[])getChildren_(EMPTY_CONTEXT, monitor);
    }

    @Override
    public final int getOccurrenceCount_()
    {
        return occurrenceCount;
    }

    @Override
    public final void setOccurrenceCount_(int occurrenceCount)
    {
        if (occurrenceCount < 1)
            throw new IllegalArgumentException();
        this.occurrenceCount = occurrenceCount;
    }

    @Override
    public IModelManager getModelManager_()
    {
        return ((LanguageElement)getParent_()).getModelManager_();
    }
}
