/*******************************************************************************
 * Copyright (c) 2019, 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.references;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.search.ui.ISearchQuery;
import org.lxtk.AbstractPartialResultProgress;
import org.lxtk.DocumentService;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.ReferenceProvider;
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.internal.ui.AbstractLocationSearchQuery;
import org.lxtk.lx4e.internal.ui.TaskExecutor;
import org.lxtk.lx4e.requests.ReferencesRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of an {@link ISearchQuery} that uses {@link ReferenceProvider}s
 * to find references to the symbol denoted by a given text document position.
 */
public class ReferenceSearchQuery
    extends AbstractLocationSearchQuery
{
    private final LanguageOperationTarget target;
    private final Position position;
    private final boolean includeDeclaration;

    /**
     * Constructor.
     *
     * @param target the {@link LanguageOperationTarget} for this search query
     *  (not <code>null</code>)
     * @param position the target text document position (not <code>null</code>)
     * @param documentService a {@link DocumentService} (not <code>null</code>)
     * @param includeDeclaration whether to include the declaration of the symbol
     *  denoted by the given text document position
     */
    public ReferenceSearchQuery(LanguageOperationTarget target, Position position,
        DocumentService documentService, boolean includeDeclaration)
    {
        super(documentService);
        this.target = Objects.requireNonNull(target);
        this.position = Objects.requireNonNull(position);
        this.includeDeclaration = includeDeclaration;
    }

    public boolean canRun()
    {
        return getReferenceProviders(target).length > 0;
    }

    @Override
    protected IStatus execute(Consumer<? super Location> acceptor, IProgressMonitor monitor)
    {
        ReferenceProvider[] providers = getReferenceProviders(target);
        if (providers.length == 0)
            return Status.OK_STATUS;

        ReferenceParams params = new ReferenceParams();
        params.setTextDocument(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()));
        params.setPosition(position);
        params.setContext(new ReferenceContext(includeDeclaration));

        return findReferences(providers, params, acceptor, monitor);
    }

    /**
     * Returns the reference providers that match the given target.
     *
     * @param target never <code>null</code>
     * @return the matching reference providers (not <code>null</code>)
     */
    protected ReferenceProvider[] getReferenceProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getMatches(
            languageService.getReferenceProviders(), ReferenceProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(ReferenceProvider[]::new);
    }

    /**
     * Finds references for the given {@link ReferenceParams} using the given reference providers
     * and reports the search results to the given acceptor.
     *
     * @param providers never <code>null</code>
     * @param params never <code>null</code>
     * @param acceptor never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the status after completion of the search
     */
    protected IStatus findReferences(ReferenceProvider[] providers, ReferenceParams params,
        Consumer<? super Location> acceptor, IProgressMonitor monitor)
    {
        TaskExecutor.parallelExecute(providers,
            (provider, taskMonitor) -> findReferences(provider, params, acceptor, taskMonitor),
            getLabel(), null, monitor);

        return Status.OK_STATUS;
    }

    /**
     * Finds references for the given {@link ReferenceParams} using the given reference provider
     * and reports the search results to the given acceptor.
     *
     * @param provider never <code>null</code>
     * @param params never <code>null</code>
     * @param acceptor never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the status after completion of the search
     */
    protected IStatus findReferences(ReferenceProvider provider, ReferenceParams params,
        Consumer<? super Location> acceptor, IProgressMonitor monitor)
    {
        ReferencesRequest request = newReferencesRequest();
        request.setProvider(provider);
        // note that request params can get modified as part of request processing
        // (e.g. a progress token can be set); therefore we need to copy the given params
        request.setParams(JsonUtil.deepCopy(params));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
        request.setUpPartialResultProgress(
            () -> new AbstractPartialResultProgress<List<? extends Location>>()
            {
                @Override
                protected void onAccept(List<? extends Location> locations)
                {
                    for (Location location : locations)
                        acceptor.accept(location);
                }
            });
        request.setMayThrow(false);

        List<? extends Location> locations = request.sendAndReceive();

        if (locations == null)
            return Status.OK_STATUS;

        for (Location location : locations)
            acceptor.accept(location);

        return Status.OK_STATUS;
    }

    /**
     * Returns a new instance of {@link ReferencesRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected ReferencesRequest newReferencesRequest()
    {
        return new ReferencesRequest();
    }

    @Override
    public String getLabel()
    {
        return Messages.ReferenceSearchQuery_Label;
    }

    @Override
    public String getResultLabel(int nMatches)
    {
        return nMatches == 1 ? Messages.ReferenceSearchQuery_Result_label_singular
            : MessageFormat.format(Messages.ReferenceSearchQuery_Result_label_plural, nMatches);
    }

    @Override
    public boolean canRerun()
    {
        // The current implementation doesn't support re-running
        return false;
    }
}
