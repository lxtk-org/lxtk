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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.graphics.Image;
import org.lxtk.CompletionProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;

/**
 * TODO JavaDoc
 */
// TODO The current implementation is superficial and needs much more work
// In particular:
// - Snippets are currently inserted as 'plain text'
// - 'CompletionList.isIncomplete' is currently ignored
// - Filtering of completion items is currently not supported
// - Context information is currently not supported
// - Auto-activation is currently not supported
// - Aggregation of completion providers is currently not supported
//   (the best matching provider is used)
// - Documentation in markdown is currently displayed as 'plain text'
public class ContentAssistProcessor
    implements IContentAssistProcessor
{
    private static final ICompletionProposal[] NO_PROPOSALS =
        new ICompletionProposal[0];
    private static final Comparator<CompletionItem> COMPLETION_COMPARATOR =
        new Comparator<CompletionItem>()
        {
            @Override
            public int compare(CompletionItem a, CompletionItem b)
            {
                if (a.getSortText() != null && b.getSortText() != null)
                {
                    int value = a.getSortText().compareToIgnoreCase(
                        b.getSortText());
                    if (value != 0)
                        return value;
                }
                int value = a.getLabel().compareTo(b.getLabel());
                if (value != 0)
                    return value;
                return a.getKind().compareTo(b.getKind());
            };
        };

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
        CompletionProvider completionProvider =
            target.getLanguageService().getDocumentMatcher().getBestMatch(
                target.getLanguageService().getCompletionProviders(),
                CompletionProvider::getDocumentSelector,
                target.getDocumentUri(), target.getLanguageId());
        if (completionProvider == null)
            return null;
        IDocument document = viewer.getDocument();
        Position position;
        try
        {
            position = DocumentUtil.toPosition(document, offset);
        }
        catch (BadLocationException e)
        {
            return null;
        }
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> future =
            completionProvider.getCompletionItems(new CompletionParams(
                DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                position));
        Either<List<CompletionItem>, CompletionList> result;
        try
        {
            result = future.get(getCompletionTimeout().toMillis(),
                TimeUnit.MILLISECONDS);
        }
        catch (CancellationException | InterruptedException e)
        {
            return null;
        }
        catch (ExecutionException e)
        {
            Activator.logError(e);
            errorMessage = e.getMessage();
            return null;
        }
        catch (TimeoutException e)
        {
            errorMessage = Messages.ContentAssistProcessor_Request_timed_out;
            return null;
        }
        if (result == null)
            return null;
        List<CompletionItem> items = result.isLeft() ? result.getLeft()
            : result.getRight().getItems();
        items.sort(COMPLETION_COMPARATOR);
        List<ICompletionProposal> proposals = new ArrayList<>(items.size());
        for (CompletionItem item : items)
        {
            String replacementString;
            int replacementOffset, replacementLength;
            TextEdit textEdit = item.getTextEdit();
            if (textEdit != null)
            {
                replacementString = textEdit.getNewText();
                IRegion region;
                try
                {
                    region = DocumentUtil.toRegion(document,
                        textEdit.getRange());
                }
                catch (BadLocationException e)
                {
                    continue;
                }
                replacementOffset = region.getOffset();
                replacementLength = region.getLength();
            }
            else
            {
                replacementString = item.getInsertText();
                if (replacementString == null)
                    replacementString = item.getLabel();
                replacementOffset = offset;
                replacementLength = 0;
            }
            String documentationString = null;
            Either<String, MarkupContent> documentation =
                item.getDocumentation();
            if (documentation != null)
                documentationString = documentation.isLeft()
                    ? documentation.getLeft()
                    : documentation.getRight().getValue();
            proposals.add(new CompletionProposal(replacementString,
                replacementOffset, replacementLength,
                replacementString.length(), getImage(item), item.getLabel(),
                null, documentationString));
        }
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
     * TODO JavaDoc
     *
     * @param item never <code>null</code>
     * @return the corresponding image, or <code>null</code> if none
     */
    protected Image getImage(CompletionItem item)
    {
        return null;
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
