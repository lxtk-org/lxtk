/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.hyperlinks;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.lsp4j.Location;
import org.lxtk.DocumentService;
import org.lxtk.lx4e.internal.ui.AbstractLocationSearchQuery;

/**
 * A dummy search query that simply adds each of the given locations to the search result.
 */
public final class LocationSearchQuery
    extends AbstractLocationSearchQuery
{
    private final List<? extends Location> locations;
    private final String resultLabel;

    /**
     * Constructor.
     *
     * @param locations a list of locations (not <code>null</code>)
     * @param resultLabel the search result label (not <code>null</code>)
     * @param documentService a {@link DocumentService} (not <code>null</code>)
     */
    public LocationSearchQuery(List<? extends Location> locations, String resultLabel,
        DocumentService documentService)
    {
        super(documentService);
        this.locations = Objects.requireNonNull(locations);
        this.resultLabel = Objects.requireNonNull(resultLabel);
    }

    @Override
    public String getLabel()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getResultLabel(int nMatches)
    {
        return resultLabel;
    }

    @Override
    public boolean canRerun()
    {
        return false;
    }

    @Override
    protected IStatus execute(Consumer<? super Location> acceptor, IProgressMonitor monitor)
    {
        for (Location location : locations)
        {
            acceptor.accept(location);
        }
        return Status.OK_STATUS;
    }
}
