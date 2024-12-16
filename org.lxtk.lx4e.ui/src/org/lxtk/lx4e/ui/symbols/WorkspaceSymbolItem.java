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

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.Assert;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolTag;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolLocation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.WorkspaceSymbolProvider;
import org.lxtk.lx4e.requests.WorkspaceSymbolResolveRequest;

/**
 * Wraps a workspace symbol.
 */
@SuppressWarnings("deprecation")
public class WorkspaceSymbolItem
{
    private final Either<SymbolInformation, WorkspaceSymbol> symbol;
    private WorkspaceSymbolProvider provider;

    /**
     * Constructor.
     *
     * @param symbol not <code>null</code>
     */
    public WorkspaceSymbolItem(SymbolInformation symbol)
    {
        this.symbol = Either.forLeft(Objects.requireNonNull(symbol));
    }

    /**
     * Constructor.
     *
     * @param symbol not <code>null</code>
     */
    public WorkspaceSymbolItem(WorkspaceSymbol symbol)
    {
        this.symbol = Either.forRight(Objects.requireNonNull(symbol));
    }

    /**
     * Constructor.
     *
     * @param symbol not <code>null</code>
     */
    public WorkspaceSymbolItem(Either<SymbolInformation, WorkspaceSymbol> symbol)
    {
        Assert.isTrue(symbol.isLeft() || symbol.isRight());
        this.symbol = symbol;
    }

    /**
     * Sets the workspace symbol provider.
     *
     * @param provider may be <code>null</code>
     */
    public final void setWorkspaceSymbolProvider(WorkspaceSymbolProvider provider)
    {
        this.provider = provider;
    }

    /**
     * Returns the workspace symbol provider.
     *
     * @return the workspace symbol provider, or <code>null</code> if none
     */
    public final WorkspaceSymbolProvider getWorkspaceSymbolProvider()
    {
        return provider;
    }

    /**
     * Returns the name of the symbol.
     *
     * @return the symbol name (never <code>null</code>)
     */
    public final String getName()
    {
        return symbol.map(SymbolInformation::getName, WorkspaceSymbol::getName);
    }

    /**
     * Returns the kind of the symbol.
     *
     * @return the symbol kind (never <code>null</code>)
     */
    public final SymbolKind getKind()
    {
        return symbol.map(SymbolInformation::getKind, WorkspaceSymbol::getKind);
    }

    /**
     * Returns the tags for the symbol.
     *
     * @return the symbol tags (may be <code>null</code> or empty)
     */
    public final List<SymbolTag> getTags()
    {
        return symbol.map(SymbolInformation::getTags, WorkspaceSymbol::getTags);
    }

    /**
     * Returns the location of the symbol.
     *
     * @return the symbol location (never <code>null</code>)
     */
    public final Either<Location, WorkspaceSymbolLocation> getLocation()
    {
        if (symbol.isRight())
            return symbol.getRight().getLocation();

        return Either.forLeft(symbol.getLeft().getLocation());
    }

    /**
     * Returns the location of the symbol, attempting to resolve it if necessary.
     *
     * @return the symbol location (never <code>null</code>)
     */
    public final Either<Location, WorkspaceSymbolLocation> getResolvedLocation()
    {
        if (symbol.isLeft())
            return Either.forLeft(symbol.getLeft().getLocation());

        WorkspaceSymbol workspaceSymbol = symbol.getRight();

        Either<Location, WorkspaceSymbolLocation> location = workspaceSymbol.getLocation();
        if (location.isLeft())
            return location;

        return resolveWorkspaceSymbol(workspaceSymbol).getLocation();
    }

    /**
     * Returns the name of the containing symbol.
     *
     * @return the name of the containing symbol (may be <code>null</code>)
     */
    public final String getContainerName()
    {
        return symbol.map(SymbolInformation::getContainerName, WorkspaceSymbol::getContainerName);
    }

    /**
     * Returns whether the symbol is deprecated.
     *
     * @return <code>true</code> if the symbol is deprecated, and <code>false</code> otherwise
     */
    public final boolean isDeprecated()
    {
        List<SymbolTag> tags = getTags();

        if (tags == null)
        {
            if (symbol.isLeft())
                return Boolean.TRUE.equals(symbol.getLeft().getDeprecated());

            return false;
        }

        return tags.contains(SymbolTag.Deprecated);
    }

    /**
     * Returns a new instance of {@link WorkspaceSymbolResolveRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected WorkspaceSymbolResolveRequest newWorkspaceSymbolResolveRequest()
    {
        return new WorkspaceSymbolResolveRequest();
    }

    /**
     * Returns the timeout for a workspace symbol resolve request.
     *
     * @return a positive duration
     */
    protected Duration getWorkspaceSymbolResolveTimeout()
    {
        return Duration.ofSeconds(1);
    }

    private WorkspaceSymbol resolveWorkspaceSymbol(WorkspaceSymbol symbol)
    {
        if (provider == null
            || !Boolean.TRUE.equals(provider.getRegistrationOptions().getResolveProvider()))
            return symbol;

        WorkspaceSymbolResolveRequest request = newWorkspaceSymbolResolveRequest();
        request.setProvider(provider);
        request.setParams(symbol);
        request.setDefaultResult(symbol);
        request.setTimeout(getWorkspaceSymbolResolveTimeout());
        request.setMayThrow(false);
        return request.sendAndReceive();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.symbol);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final WorkspaceSymbolItem other = (WorkspaceSymbolItem)obj;
        return Objects.equals(this.symbol, other.symbol);
    }
}
