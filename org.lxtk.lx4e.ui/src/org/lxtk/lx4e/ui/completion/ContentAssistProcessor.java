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
package org.lxtk.lx4e.ui.completion;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.graphics.Image;
import org.lxtk.CompletionProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.LSPImages;

/**
 * TODO JavaDoc
 */
// TODO The current implementation needs more work
// In particular:
// - Context information is currently not supported
// - Auto-activation is currently not supported
// - Aggregation of completion providers is currently not supported
//   (the best matching provider is used)
// - Snippet support is ad hoc
public class ContentAssistProcessor
    implements IContentAssistProcessor
{
    private static final ICompletionProposal[] NO_PROPOSALS =
        new ICompletionProposal[0];

    private final Supplier<LanguageOperationTarget> targetSupplier;
    private String errorMessage;

    /**
     * TODO JavaDoc
     *
     * @param targetSupplier not <code>null</code>
     */
    public ContentAssistProcessor(
        Supplier<LanguageOperationTarget> targetSupplier)
    {
        this.targetSupplier = Objects.requireNonNull(targetSupplier);
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
        int offset)
    {
        LanguageOperationTarget target = targetSupplier.get();
        if (target == null)
            return null;
        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();
        CompletionProvider provider =
            languageService.getDocumentMatcher().getBestMatch(
                languageService.getCompletionProviders(),
                CompletionProvider::getDocumentSelector, documentUri,
                target.getLanguageId());
        if (provider == null)
            return null;
        IDocument document = viewer.getDocument();
        Position position;
        try
        {
            position = DocumentUtil.toPosition(document, offset);
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> future =
            provider.getCompletionItems(new CompletionParams(
                DocumentUri.toTextDocumentIdentifier(documentUri), position));
        Either<List<CompletionItem>, CompletionList> result = null;
        try
        {
            result = future.get(getCompletionTimeout().toMillis(),
                TimeUnit.MILLISECONDS);
        }
        catch (CancellationException | InterruptedException e)
        {
        }
        catch (ExecutionException e)
        {
            Activator.logError(e);
            errorMessage = e.getMessage();
        }
        catch (TimeoutException e)
        {
            Activator.logWarning(e);
            errorMessage = Messages.ContentAssistProcessor_Request_timed_out;
        }
        if (result == null)
            return null;
        List<CompletionItem> items;
        boolean isIncomplete = false;
        if (result.isLeft())
            items = result.getLeft();
        else
        {
            isIncomplete = result.getRight().isIncomplete();
            items = result.getRight().getItems();
        }
        List<ICompletionProposal> proposals = new ArrayList<>(items.size());
        for (CompletionItem item : items)
        {
            ICompletionProposal proposal = isIncomplete
                ? new LSIncompleteCompletionProposal(documentUri, document,
                    offset, item, provider, getImage(item))
                : new LSCompletionProposal(documentUri, document, offset, item,
                    provider, getImage(item));
            proposals.add(proposal);
        }
        proposals = filterAndSortCompletionProposals(proposals);
        return proposals.toArray(NO_PROPOSALS);
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer,
        int offset)
    {
        return null;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters()
    {
        return null;
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters()
    {
        return null;
    }

    @Override
    public String getErrorMessage()
    {
        return errorMessage;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator()
    {
        return null;
    }

    /**
     * Filters and sorts the proposals. The passed list may be modified
     * and returned, or a new list may be created and returned.
     *
     * @param proposals the list of collected proposals (never <code>null</code>)
     * @return the list of filtered and sorted proposals, ready for display
     *  (not <code>null</code>)
     */
    protected List<ICompletionProposal> filterAndSortCompletionProposals(
        List<ICompletionProposal> proposals)
    {
        return proposals;
    }

    /**
     * TODO JavaDoc
     *
     * @param item never <code>null</code>
     * @return the corresponding image, or <code>null</code> if none
     */
    protected Image getImage(CompletionItem item)
    {
        return LSPImages.imageFromCompletionItem(item);
    }

    /**
     * TODO JavaDoc
     *
     * @return a positive duration
     */
    protected Duration getCompletionTimeout()
    {
        return Duration.ofSeconds(1);
    }
}
