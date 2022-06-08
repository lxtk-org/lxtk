/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.symbols;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolTag;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolLocation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Wraps a workspace symbol.
 */
@SuppressWarnings("deprecation")
public class WorkspaceSymbolItem
{
    private final Either<WorkspaceSymbol, SymbolInformation> symbol;

    /**
     * Constructor.
     *
     * @param symbol not <code>null</code>
     */
    public WorkspaceSymbolItem(WorkspaceSymbol symbol)
    {
        this.symbol = Either.forLeft(Objects.requireNonNull(symbol));
    }

    /**
     * Constructor.
     *
     * @param symbol not <code>null</code>
     */
    public WorkspaceSymbolItem(SymbolInformation symbol)
    {
        this.symbol = Either.forRight(Objects.requireNonNull(symbol));
    }

    /**
     * Returns the name of the symbol.
     *
     * @return the symbol name (never <code>null</code>)
     */
    public String getName()
    {
        return symbol.map(WorkspaceSymbol::getName, SymbolInformation::getName);
    }

    /**
     * Returns the kind of the symbol.
     *
     * @return the symbol kind (never <code>null</code>)
     */
    public SymbolKind getKind()
    {
        return symbol.map(WorkspaceSymbol::getKind, SymbolInformation::getKind);
    }

    /**
     * Returns the tags for the symbol.
     *
     * @return the symbol tags (may be <code>null</code> or empty)
     */
    public List<SymbolTag> getTags()
    {
        return symbol.map(WorkspaceSymbol::getTags, SymbolInformation::getTags);
    }

    /**
     * Returns the location of the symbol.
     *
     * @return the symbol location (never <code>null</code>)
     */
    public Either<Location, WorkspaceSymbolLocation> getLocation()
    {
        if (symbol.isLeft())
            return symbol.getLeft().getLocation();

        return Either.forLeft(symbol.getRight().getLocation());
    }

    /**
     * Returns the name of the containing symbol.
     *
     * @return the name of the containing symbol (may be <code>null</code>)
     */
    public String getContainerName()
    {
        return symbol.map(WorkspaceSymbol::getContainerName, SymbolInformation::getContainerName);
    }

    /**
     * Returns whether the symbol is deprecated.
     *
     * @return <code>true</code> if the symbol is deprecated, and <code>false</code> otherwise
     */
    public boolean isDeprecated()
    {
        List<SymbolTag> tags = getTags();

        if (tags == null)
        {
            if (symbol.isRight())
                return Boolean.TRUE.equals(symbol.getRight().getDeprecated());

            return false;
        }

        return tags.contains(SymbolTag.Deprecated);
    }
}
