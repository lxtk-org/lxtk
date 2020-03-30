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
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.graphics.Image;
import org.lxtk.CompletionProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.SignatureHelpProvider;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.LSPImages;

/**
 * TODO JavaDoc
 */
// Implementation limits:
// - Snippet support is ad hoc (inherited from LSP4E)
// - Aggregation of completion providers is not supported
//   (the best matching provider is used)
// - Auto-activation by trigger characters specified in the server options
//   is not supported (a subclass can explicitly specify auto-activation
//   characters by overriding the corresponding methods)
public class ContentAssistProcessor
    implements IContentAssistProcessor
{
    private static final ICompletionProposal[] NO_PROPOSALS =
        new ICompletionProposal[0];
    private static final IContextInformation[] NO_INFOS =
        new IContextInformation[0];

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
        errorMessage = null;
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
        return proposals.toArray(NO_PROPOSALS);
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer,
        int offset)
    {
        errorMessage = null;
        LanguageOperationTarget target = targetSupplier.get();
        if (target == null)
            return NO_INFOS;
        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();
        SignatureHelpProvider provider =
            languageService.getDocumentMatcher().getBestMatch(
                languageService.getSignatureHelpProviders(),
                SignatureHelpProvider::getDocumentSelector, documentUri,
                target.getLanguageId());
        if (provider == null)
            return NO_INFOS;
        IDocument document = viewer.getDocument();
        Position position;
        try
        {
            position = DocumentUtil.toPosition(document, offset);
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return NO_INFOS;
        }
        CompletableFuture<SignatureHelp> future = provider.getSignatureHelp(
            new TextDocumentPositionParams(DocumentUri.toTextDocumentIdentifier(
                documentUri), position));
        SignatureHelp result = null;
        try
        {
            result = future.get(getContextInformationTimeout().toMillis(),
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
            return NO_INFOS;
        List<SignatureInformation> signatures = result.getSignatures();
        int size = signatures.size();
        int activeSignature;
        if (result.getActiveSignature() == null)
            activeSignature = 0;
        else
        {
            activeSignature = result.getActiveSignature().intValue();
            if (activeSignature < 0 || activeSignature >= size)
                activeSignature = 0;
        }
        List<IContextInformation> infos = new ArrayList<>(size);
        int index = 0;
        for (SignatureInformation signature : signatures)
        {
            IContextInformation info = toContextInformation(signature);
            if (info != null)
            {
                if (index == activeSignature)
                    infos.add(0, info);
                else
                    infos.add(info);
            }
            index++;
        }
        return infos.toArray(NO_INFOS);
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
        return new ContextInformationValidator(this);
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
     * @param signature never <code>null</code>
     * @return a context information for the given signature,
     *  or <code>null</code> if none
     */
    protected IContextInformation toContextInformation(
        SignatureInformation signature)
    {
        return new ContextInformation(signature.getLabel(),
            signature.getLabel());
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

    /**
     * TODO JavaDoc
     *
     * @return a positive duration
     */
    protected Duration getContextInformationTimeout()
    {
        return Duration.ofSeconds(1);
    }
}
