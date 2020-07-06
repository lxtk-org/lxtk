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
import java.util.function.Supplier;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpContext;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SignatureHelpTriggerKind;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple.Two;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.lxtk.CompletionProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.SignatureHelpProvider;
import org.lxtk.lx4e.CompletionRequest;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.SignatureHelpRequest;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.LSPImages;

/**
 * Default implementation of an {@link IContentAssistProcessor} that
 * computes completion proposals using a {@link CompletionProvider} and
 * context information using a {@link SignatureHelpProvider}.
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
    private static final ICompletionProposal[] NO_PROPOSALS = new ICompletionProposal[0];
    private static final IContextInformation[] NO_INFOS = new IContextInformation[0];

    private final Supplier<LanguageOperationTarget> targetSupplier;
    private String errorMessage;
    private IContextInformationValidator contextInformationValidator;

    /**
     * Constructor.
     *
     * @param targetSupplier the {@link LanguageOperationTarget} supplier
     *  for this processor (not <code>null</code>)
     */
    public ContentAssistProcessor(Supplier<LanguageOperationTarget> targetSupplier)
    {
        this.targetSupplier = Objects.requireNonNull(targetSupplier);
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset)
    {
        errorMessage = null;

        LanguageOperationTarget target = targetSupplier.get();
        if (target == null)
            return null;

        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();

        CompletionProvider provider = languageService.getDocumentMatcher().getBestMatch(
            languageService.getCompletionProviders(), CompletionProvider::getDocumentSelector,
            documentUri, target.getLanguageId());
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

        CompletionRequest request = newCompletionRequest();
        request.setProvider(provider);
        request.setParams(
            new CompletionParams(DocumentUri.toTextDocumentIdentifier(documentUri), position));
        request.setTimeout(getCompletionTimeout());
        request.setMayThrow(false);

        Either<List<CompletionItem>, CompletionList> result = request.sendAndReceive();
        errorMessage = request.getErrorMessage();

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
                ? new LSIncompleteCompletionProposal(documentUri, document, offset, item, provider,
                    getImage(item))
                : new LSCompletionProposal(documentUri, document, offset, item, provider,
                    getImage(item));
            proposals.add(proposal);
        }
        return proposals.toArray(NO_PROPOSALS);
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset)
    {
        errorMessage = null;

        SignatureHelpRequest request = getSignatureHelpRequest(viewer, offset, null);
        if (request == null)
            return NO_INFOS;

        SignatureHelp result = request.sendAndReceive();
        errorMessage = request.getErrorMessage();

        if (result == null)
            return NO_INFOS;

        normalize(result);

        int size = result.getSignatures().size();
        List<IContextInformation> infos = new ArrayList<>(size);
        for (int index = 0; index < size; index++)
        {
            IContextInformation info = toContextInformation(result, index);
            if (info != null)
            {
                if (index == result.getActiveSignature())
                    infos.add(0, info);
                else
                    infos.add(info);
            }
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
        if (contextInformationValidator == null)
            contextInformationValidator = newContextInformationValidator();
        return contextInformationValidator;
    }

    /**
     * Returns a new instance of {@link IContextInformationValidator}.
     *
     * @return the created validator (not <code>null</code>)
     */
    protected IContextInformationValidator newContextInformationValidator()
    {
        return new SignatureContextInformationValidator();
    }

    /**
     * Returns the image that corresponds to the given completion item.
     *
     * @param item never <code>null</code>
     * @return the corresponding image, or <code>null</code> if none
     */
    protected Image getImage(CompletionItem item)
    {
        return LSPImages.imageFromCompletionItem(item);
    }

    /**
     * Converts the given signature to a context information object.
     *
     * @param signatureHelp a {@link SignatureHelp} (never <code>null</code>)
     * @param signatureIndex a valid index into <code>signatureHelp.getSignatures()</code>
     * @return the corresponding context information, or <code>null</code> if none
     */
    protected IContextInformation toContextInformation(SignatureHelp signatureHelp,
        int signatureIndex)
    {
        return new SignatureContextInformation(signatureHelp, signatureIndex);
    }

    /**
     * Returns a new instance of {@link CompletionRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected CompletionRequest newCompletionRequest()
    {
        return new CompletionRequest();
    }

    /**
     * Returns the timeout for a completion request.
     *
     * @return a positive duration
     */
    protected Duration getCompletionTimeout()
    {
        return Duration.ofSeconds(1);
    }

    /**
     * Returns a new instance of {@link SignatureHelpRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected SignatureHelpRequest newSignatureHelpRequest()
    {
        return new SignatureHelpRequest();
    }

    /**
     * Returns the timeout for a signature help request.
     *
     * @return a positive duration
     */
    protected Duration getSignatureHelpTimeout()
    {
        return Duration.ofSeconds(1);
    }

    /**
     * Returns whether the signature's active parameter should be highlighted.
     * <p>
     * Note that even if this method returns <code>true</code>, it might not be feasible
     * to highlight the signature's active parameter in some cases, depending on the
     * {@link SignatureHelpProvider} implementation. That would be the case, for example,
     * if the signature help provider does not support a {@link SignatureHelpContext}.
     * </p>
     *
     * @return <code>true</code> if the signature's active parameter should be highlighted,
     *  and <code>false</code> otherwise
     */
    protected boolean shouldMarkSignatureActiveParameter()
    {
        return true;
    }

    private SignatureHelpRequest getSignatureHelpRequest(ITextViewer viewer, int offset,
        SignatureHelpContext context)
    {
        LanguageOperationTarget target = targetSupplier.get();
        if (target == null)
            return null;

        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();

        SignatureHelpProvider provider = languageService.getDocumentMatcher().getBestMatch(
            languageService.getSignatureHelpProviders(), SignatureHelpProvider::getDocumentSelector,
            documentUri, target.getLanguageId());
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

        SignatureHelpRequest request = newSignatureHelpRequest();
        request.setProvider(provider);
        request.setParams(new SignatureHelpParams(DocumentUri.toTextDocumentIdentifier(documentUri),
            position, context));
        request.setTimeout(getSignatureHelpTimeout());
        request.setMayThrow(false);
        return request;
    }

    private static void normalize(SignatureHelp signatureHelp)
    {
        if (signatureHelp.getActiveSignature() == null)
            signatureHelp.setActiveSignature(0);
        else
        {
            int activeSignature = signatureHelp.getActiveSignature().intValue();
            if (activeSignature < 0 || activeSignature >= signatureHelp.getSignatures().size())
                signatureHelp.setActiveSignature(0);
        }

        if (signatureHelp.getActiveParameter() == null)
            signatureHelp.setActiveParameter(0);
        else
        {
            int activeParameter = signatureHelp.getActiveParameter().intValue();
            if (activeParameter < 0 || activeParameter >= signatureHelp.getSignatures().get(
                signatureHelp.getActiveSignature()).getParameters().size())
                signatureHelp.setActiveParameter(0);
        }
    }

    private static class SignatureContextInformation
        implements IContextInformation
    {
        final SignatureHelp signatureHelp;
        final int signatureIndex;
        final SignatureInformation signature;
        final String label;

        SignatureContextInformation(SignatureHelp signatureHelp, int signatureIndex)
        {
            this.signatureHelp = signatureHelp;
            this.signatureIndex = signatureIndex;
            signature = signatureHelp.getSignatures().get(signatureIndex);
            label = signature.getLabel();
        }

        @Override
        public String getContextDisplayString()
        {
            return label;
        }

        @Override
        public Image getImage()
        {
            return null;
        }

        @Override
        public String getInformationDisplayString()
        {
            return label;
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
            SignatureContextInformation other = (SignatureContextInformation)obj;
            return label.equals(other.label);
        }

        @Override
        public int hashCode()
        {
            return label.hashCode();
        }
    }

    private class SignatureContextInformationValidator
        implements IContextInformationValidator, IContextInformationPresenter
    {
        private SignatureContextInformation info;
        private ITextViewer viewer;
        private SignatureHelp signatureHelp;
        private ParameterInformation currentParameter;
        private boolean markActiveParameter;

        @Override
        public void install(IContextInformation info, ITextViewer viewer, int offset)
        {
            this.info = (SignatureContextInformation)info;
            this.viewer = viewer;
            signatureHelp = null;
            currentParameter = null;
            markActiveParameter = shouldMarkSignatureActiveParameter();
        }

        @Override
        public boolean isContextInformationValid(int offset)
        {
            signatureHelp = computeSignatureHelp(offset);
            if (signatureHelp == null)
                return false;

            return signatureHelp.getSignatures().contains(info.signature);
        }

        @Override
        public boolean updatePresentation(int offset, TextPresentation presentation)
        {
            if (!markActiveParameter)
                return false;

            if (signatureHelp == null)
                signatureHelp = computeSignatureHelp(offset);
            if (signatureHelp == null)
            {
                if (currentParameter == null)
                    return false;

                presentation.clear();
                return true;
            }

            SignatureInformation activeSignature =
                signatureHelp.getSignatures().get(signatureHelp.getActiveSignature());
            if (!info.signature.equals(activeSignature))
            {
                if (currentParameter == null)
                    return false;

                presentation.clear();
                return true;
            }

            ParameterInformation activeParameter =
                activeSignature.getParameters().get(signatureHelp.getActiveParameter());
            if (activeParameter.equals(currentParameter))
                return false;

            currentParameter = activeParameter;

            Two<Integer, Integer> offsets = getOffsets(activeParameter);
            if (offsets == null)
            {
                presentation.clear();
                return true;
            }

            int start = offsets.getFirst();
            int end = offsets.getSecond();

            presentation.clear();

            if (start > 0)
                presentation.addStyleRange(new StyleRange(0, start, null, null, SWT.NORMAL));

            if (end > start)
                presentation.addStyleRange(
                    new StyleRange(start, end - start, null, null, SWT.BOLD));

            if (end < info.label.length())
                presentation.addStyleRange(
                    new StyleRange(end, info.label.length() - end, null, null, SWT.NORMAL));

            return true;
        }

        private SignatureHelp computeSignatureHelp(int offset)
        {
            SignatureHelpContext context = new SignatureHelpContext();
            context.setTriggerKind(SignatureHelpTriggerKind.ContentChange);
            context.setIsRetrigger(true);
            context.setActiveSignatureHelp(info.signatureHelp);
            info.signatureHelp.setActiveSignature(info.signatureIndex);

            SignatureHelpRequest request = getSignatureHelpRequest(viewer, offset, context);
            if (request == null)
                return null;

            SignatureHelp result = request.sendAndReceive();
            if (result != null)
                normalize(result);
            return result;
        }

        private Two<Integer, Integer> getOffsets(ParameterInformation parameter)
        {
            Either<String, Two<Integer, Integer>> label = parameter.getLabel();
            Two<Integer, Integer> offsets = null;
            if (label.isRight())
                offsets = label.getRight();
            else if (label.isLeft())
            {
                int index = info.label.indexOf(label.getLeft());
                if (index >= 0)
                    offsets = new Two<>(index, index + label.getLeft().length());
            }
            return offsets;
        }
    }
}
