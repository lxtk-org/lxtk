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
package org.lxtk.lx4e.ui.typehierarchy;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
import org.lxtk.AbstractPartialResultProgress;
import org.lxtk.TypeHierarchyProvider;
import org.lxtk.lx4e.IUriHandler;
import org.lxtk.lx4e.requests.TypeHierarchySubtypesRequest;
import org.lxtk.lx4e.requests.TypeHierarchySupertypesRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Provides information about sub-types/super-types.
 */
public class TypeHierarchyUtility
{
    protected final TypeHierarchyProvider provider;
    protected final IUriHandler uriHandler;

    /**
     * Constructor.
     *
     * @param provider not <code>null</code>
     * @param uriHandler not <code>null</code>
     */
    public TypeHierarchyUtility(TypeHierarchyProvider provider, IUriHandler uriHandler)
    {
        this.provider = Objects.requireNonNull(provider);
        this.uriHandler = Objects.requireNonNull(uriHandler);
    }

    /**
     * Fetches the sub-types for the given type hierarchy item into the given acceptor.
     *
     * @param item not <code>null</code>
     * @param acceptor not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     */
    public void fetchSubtypes(TypeHierarchyItem item, Consumer<List<TypeHierarchyItem>> acceptor,
        IProgressMonitor monitor)
    {
        TypeHierarchySubtypesRequest request = newSubtypesRequest();
        request.setProvider(provider);
        request.setParams(new TypeHierarchySubtypesParams(item));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
        request.setUpPartialResultProgress(
            () -> new AbstractPartialResultProgress<List<TypeHierarchyItem>>()
            {
                @Override
                protected void onAccept(List<TypeHierarchyItem> items)
                {
                    acceptor.accept(items);
                }
            });
        request.setMayThrow(false);

        List<TypeHierarchyItem> items = request.sendAndReceive();
        if (items != null)
            acceptor.accept(items);
    }

    /**
     * Fetches the super-types for the given type hierarchy item into the given acceptor.
     *
     * @param item not <code>null</code>
     * @param acceptor not <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     */
    public void fetchSupertypes(TypeHierarchyItem item, Consumer<List<TypeHierarchyItem>> acceptor,
        IProgressMonitor monitor)
    {
        TypeHierarchySupertypesRequest request = newSupertypesRequest();
        request.setProvider(provider);
        request.setParams(new TypeHierarchySupertypesParams(item));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
        request.setUpPartialResultProgress(
            () -> new AbstractPartialResultProgress<List<TypeHierarchyItem>>()
            {
                @Override
                protected void onAccept(List<TypeHierarchyItem> items)
                {
                    acceptor.accept(items);
                }
            });
        request.setMayThrow(false);

        List<TypeHierarchyItem> items = request.sendAndReceive();
        if (items != null)
            acceptor.accept(items);
    }

    /**
     * Returns a new instance of {@link TypeHierarchySubtypesRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected TypeHierarchySubtypesRequest newSubtypesRequest()
    {
        return new TypeHierarchySubtypesRequest();
    }

    /**
     * Returns a new instance of {@link TypeHierarchySupertypesRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected TypeHierarchySupertypesRequest newSupertypesRequest()
    {
        return new TypeHierarchySupertypesRequest();
    }
}
