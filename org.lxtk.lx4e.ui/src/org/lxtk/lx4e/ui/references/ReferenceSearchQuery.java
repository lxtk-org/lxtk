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
package org.lxtk.lx4e.ui.references;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.search.ui.ISearchQuery;
import org.lxtk.DocumentService;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.ReferenceProvider;
import org.lxtk.lx4e.ReferencesRequest;
import org.lxtk.lx4e.internal.ui.AbstractLocationSearchQuery;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of an {@link ISearchQuery} that uses a {@link ReferenceProvider}
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
        return getReferenceProvider() != null;
    }

    @Override
    protected IStatus execute(Consumer<? super Location> acceptor, IProgressMonitor monitor)
    {
        ReferenceProvider provider = getReferenceProvider();
        if (provider == null)
            return Status.OK_STATUS;

        ReferenceParams params = new ReferenceParams();
        params.setTextDocument(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()));
        params.setPosition(position);
        params.setContext(new ReferenceContext(includeDeclaration));

        ReferencesRequest request = newReferencesRequest();
        request.setProvider(provider);
        request.setParams(params);
        request.setProgressMonitor(monitor);
        if (Boolean.TRUE.equals(provider.getRegistrationOptions().getWorkDoneProgress()))
            request.setWorkDoneProgress(WorkDoneProgressFactory.newWorkDoneProgress());

        List<? extends Location> locations;
        try
        {
            locations = request.sendAndReceive();
        }
        catch (CompletionException e)
        {
            return Activator.createErrorStatus(request.getErrorMessage(), e.getCause());
        }

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

    private ReferenceProvider getReferenceProvider()
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getBestMatch(
            languageService.getReferenceProviders(), ReferenceProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId());
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
