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
package org.lxtk.lx4e.ui.completion;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.TaskExecutor;
import org.lxtk.lx4e.requests.CompletionRequest;
import org.lxtk.lx4e.requests.SignatureHelpRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;

/**
 * Default implementation of an {@link IContentAssistProcessor} that
 * computes completion proposals using {@link CompletionProvider}(s)
 * and context information using {@link SignatureHelpProvider}(s).
 */
// Implementation limits:
// - Auto-activation by trigger characters specified in the server options
//   is not supported (a subclass can explicitly specify auto-activation
//   characters by overriding the corresponding methods)
public class ContentAssistProcessor
    implements IContentAssistProcessor
{
    private static final ICompletionProposal[] NO_PROPOSALS = new ICompletionProposal[0];
    private static final IContextInformation[] NO_INFOS = new IContextInformation[0];

    private final Supplier<LanguageOperationTarget> targetSupplier;
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

    /**
     * Returns the {@link LanguageOperationTarget} for this processor.
     * <p>
     * There is no requirement that the same result be returned each time this method is invoked.
     * </p>
     *
     * @return the language operation target, or <code>null</code> if none
     */
    public LanguageOperationTarget getLanguageOperationTarget()
    {
        return targetSupplier.get();
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset)
    {
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return null;

        CompletionProvider[][] providerGroups = getCompletionProviderGroups(target);
        if (providerGroups == null || providerGroups.length == 0)
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

        CompletionParams params = new CompletionParams(
            DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()), position);

        CompletionContext context = newCompletionContext();
        context.setTextViewer(viewer);
        context.setDocumentUri(target.getDocumentUri());
        context.setInvocationOffset(offset);
        context.setContentAssistProcessor(this);

        for (CompletionProvider[] providerGroup : providerGroups)
        {
            CompletionResults results = computeCompletionResults(providerGroup, params);
            if (results != null)
            {
                ICompletionProposal[] proposals = getCompletionProposals(results, context);
                if (proposals != null && proposals.length > 0)
                    return proposals;
            }
        }

        return null;
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset)
    {
        SignatureHelpResult result = computeSignatureHelpResult(viewer, offset, null);
        if (result == null)
            return null;

        SignatureHelp signatureHelp = result.getSignatureHelp();
        if (signatureHelp == null)
            return null;

        normalize(signatureHelp);

        int size = signatureHelp.getSignatures().size();
        List<IContextInformation> infos = new ArrayList<>(size);
        for (int index = 0; index < size; index++)
        {
            IContextInformation info = toContextInformation(signatureHelp, index);
            if (info != null)
            {
                if (index == signatureHelp.getActiveSignature())
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
        return null;
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
     * Returns a new instance of {@link CompletionContext}.
     *
     * @return the created context (not <code>null</code>
     */
    protected CompletionContext newCompletionContext()
    {
        return new CompletionContext();
    }

    /**
     * Returns completion providers that match the given target, grouped by their match score,
     * in descending order.
     *
     * @param target never <code>null</code>
     * @return the matching completion providers, grouped by their match score, in descending order,
     *  or <code>null</code> if none
     */
    protected CompletionProvider[][] getCompletionProviderGroups(LanguageOperationTarget target)
    {
        List<CompletionProvider[]> result = new ArrayList<>();

        LanguageService languageService = target.getLanguageService();

        NavigableMap<Integer, List<CompletionProvider>> groupedMatches =
            languageService.getDocumentMatcher().getGroupedMatches(
                languageService.getCompletionProviders(), CompletionProvider::getDocumentSelector,
                target.getDocumentUri(), target.getLanguageId());

        for (List<CompletionProvider> group : groupedMatches.values())
        {
            result.add(group.toArray(CompletionProvider[]::new));
        }

        return result.toArray(CompletionProvider[][]::new);
    }

    /**
     * Asks each of the given completion providers to compute a result for the given
     * {@link CompletionParams} and returns the computed results.
     *
     * @param completionProviders never <code>null</code>
     * @param completionParams never <code>null</code>
     * @return the computed completion results, or <code>null</code> if none
     */
    protected CompletionResults computeCompletionResults(CompletionProvider[] completionProviders,
        CompletionParams completionParams)
    {
        if (completionProviders.length == 0)
            return null;

        return new CompletionResults(TaskExecutor.parallelCompute(completionProviders,
            (completionProvider, monitor) -> computeCompletionResult(completionProvider,
                completionParams, monitor),
            Messages.Computing_completion_proposals, getCompletionTimeout(), null));
    }

    /**
     * Asks the given completion provider to compute a result for the given
     * {@link CompletionParams} and returns the computed result.
     *
     * @param completionProvider never <code>null</code>
     * @param completionParams never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed completion result, or <code>null</code> if none
     */
    protected CompletionResult computeCompletionResult(CompletionProvider completionProvider,
        CompletionParams completionParams, IProgressMonitor monitor)
    {
        CompletionRequest request = newCompletionRequest();
        request.setProvider(completionProvider);
        // note that request params can get modified as part of request processing
        // (e.g. a progress token can be set); therefore we need to copy the given params
        request.setParams(JsonUtil.deepCopy(completionParams));
        request.setTimeout(getCompletionTimeout());
        request.setMayThrow(false);
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);

        Either<List<CompletionItem>, CompletionList> response = request.sendAndReceive();

        CompletionList completionList = response == null ? null
            : response.isLeft() ? new CompletionList(response.getLeft()) : response.getRight();

        return new CompletionResult(completionList);
    }

    /**
     * Returns completion proposals for the given completion results.
     *
     * @param completionResults never <code>null</code>
     * @param completionContext never <code>null</code>
     * @return the completion proposals, or <code>null</code> if none
     */
    protected ICompletionProposal[] getCompletionProposals(CompletionResults completionResults,
        CompletionContext completionContext)
    {
        List<ICompletionProposal> result = new ArrayList<>();

        completionResults.asMap().forEach((completionProvider, completionResult) ->
        {
            if (completionResult != null)
            {
                CompletionList completionList = completionResult.getCompletionList();
                if (completionList != null)
                {
                    for (CompletionItem completionItem : completionList.getItems())
                    {
                        ICompletionProposal completionProposal = toCompletionProposal(
                            completionItem, completionList, completionProvider, completionContext);
                        if (completionProposal != null)
                            result.add(completionProposal);
                    }
                }
            }
        });

        return result.toArray(NO_PROPOSALS);
    }

    /**
     * Converts the given completion item to a completion proposal.
     *
     * @param completionItem never <code>null</code>
     * @param completionList never <code>null</code>
     * @param completionProvider never <code>null</code>
     * @param completionContext never <code>null</code>
     * @return the corresponding completion proposal, or <code>null</code> if none
     */
    protected ICompletionProposal toCompletionProposal(CompletionItem completionItem,
        CompletionList completionList, CompletionProvider completionProvider,
        CompletionContext completionContext)
    {
        return completionList.isIncomplete()
            ? new BaseCompletionProposal(completionItem, completionProvider, completionContext)
            : new CompletionProposal(completionItem, completionProvider, completionContext);
    }

    /**
     * Computes a signature help result for the given offset in the given text viewer.
     *
     * @param viewer never <code>null</code>
     * @param offset a zero-based offset
     * @param context may be <code>null</code>
     * @return the computed signature help result, or <code>null</code> if none
     */
    protected SignatureHelpResult computeSignatureHelpResult(ITextViewer viewer, int offset,
        SignatureHelpContext context)
    {
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return null;

        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();

        List<SignatureHelpProvider> providers =
            languageService.getDocumentMatcher().getSortedMatches(
                languageService.getSignatureHelpProviders(),
                SignatureHelpProvider::getDocumentSelector, documentUri, target.getLanguageId());
        if (providers.isEmpty())
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

        SignatureHelpParams params = new SignatureHelpParams(
            DocumentUri.toTextDocumentIdentifier(documentUri), position, context);

        for (SignatureHelpProvider provider : providers)
        {
            SignatureHelpRequest request = newSignatureHelpRequest();
            request.setProvider(provider);
            // note that request params can get modified as part of request processing
            // (e.g. a progress token can be set); therefore we need to copy the original params
            request.setParams(JsonUtil.deepCopy(params));
            request.setTimeout(getSignatureHelpTimeout());
            request.setMayThrow(false);
            request.setUpWorkDoneProgress(
                () -> WorkDoneProgressFactory.newWorkDoneProgressWithJob(false));

            SignatureHelp signatureHelp = request.sendAndReceive();
            if (signatureHelp != null)
                return new SignatureHelpResult(signatureHelp);
        }

        return null;
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
     * Returns options for active parameter highlighting of a signature.
     *
     * @return options for active parameter highlighting of a signature, or <code>null</code>
     *  if active parameter highlighting is disabled
     */
    protected MarkSignatureActiveParameterOptions getMarkSignatureActiveParameterOptions()
    {
        return MarkSignatureActiveParameterOptions.DEFAULT;
    }

    private static void normalize(SignatureHelp signatureHelp)
    {
        if (signatureHelp.getActiveSignature() == null)
        {
            signatureHelp.setActiveSignature(0);
        }
        else
        {
            int activeSignature = signatureHelp.getActiveSignature().intValue();
            if (activeSignature < 0 || activeSignature >= signatureHelp.getSignatures().size())
            {
                signatureHelp.setActiveSignature(0);
            }
        }

        if (signatureHelp.getActiveParameter() == null)
        {
            signatureHelp.setActiveParameter(0);
        }
        else
        {
            int activeParameter = signatureHelp.getActiveParameter().intValue();
            if (activeParameter < 0 || activeParameter >= getParameterCount(
                signatureHelp.getSignatures().get(signatureHelp.getActiveSignature())))
            {
                signatureHelp.setActiveParameter(0);
            }
        }
    }

    private static int getParameterCount(SignatureInformation signature)
    {
        List<ParameterInformation> parameters = signature.getParameters();
        return parameters == null ? 0 : parameters.size();
    }

    private static SignatureInformation findSignatureByLabel(List<SignatureInformation> collection,
        String label)
    {
        for (SignatureInformation signature : collection)
        {
            if (signature.getLabel().equals(label))
                return signature;
        }
        return null;
    }

    /**
     * Represents completion results.
     */
    protected static class CompletionResults
    {
        private final Map<CompletionProvider, CompletionResult> results;

        /**
         * Constructor.
         *
         * @param results not <code>null</code>
         */
        public CompletionResults(Map<CompletionProvider, CompletionResult> results)
        {
            this.results = Objects.requireNonNull(results);
        }

        /**
         * Returns the completion results as a map.
         *
         * @return the completion results as a map (never <code>null</code>)
         */
        public Map<CompletionProvider, CompletionResult> asMap()
        {
            return results;
        }
    }

    /**
     * Represents a completion result.
     */
    protected static class CompletionResult
    {
        private final CompletionList completionList;

        /**
         * Constructor.
         *
         * @param completionList may be <code>null</code>
         */
        public CompletionResult(CompletionList completionList)
        {
            this.completionList = completionList;
        }

        /**
         * Returns the completion list.
         *
         * @return the completion list, or <code>null</code> if none
         */
        public CompletionList getCompletionList()
        {
            return completionList;
        }
    }

    /**
     * Represents a signature help result.
     */
    protected static class SignatureHelpResult
    {
        private final SignatureHelp signatureHelp;

        /**
         * Constructor.
         *
         * @param signatureHelp may be <code>null</code>
         */
        public SignatureHelpResult(SignatureHelp signatureHelp)
        {
            this.signatureHelp = signatureHelp;
        }

        /**
         * Returns the signature help.
         *
         * @return the signature help, or <code>null</code> if none
         */
        public SignatureHelp getSignatureHelp()
        {
            return signatureHelp;
        }
    }

    /**
     * Describes options for active parameter highlighting of a signature.
     */
    protected static final class MarkSignatureActiveParameterOptions
    {
        static final MarkSignatureActiveParameterOptions DEFAULT =
            new MarkSignatureActiveParameterOptions();

        private boolean strict;

        /**
         * Returns whether active parameter highlighting is <i>strict</i>.
         * <p>
         * Depending on {@link SignatureHelpProvider} implementation, it might not be
         * feasible to accurately highlight the active parameter of a signature in all cases.
         * In non-strict mode (the default), highlighting is performed even in those cases
         * when it is not guaranteed to be accurate, in the hope that it will be accurate
         * in most cases.
         * </p>
         *
         * @return <code>true</code> if active parameter highlighting is strict,
         *  and <code>false</code> otherwise
         */
        public boolean isStrict()
        {
            return strict;
        }

        /**
         * Sets whether active parameter highlighting is strict.
         *
         * @param strict whether active parameter highlighting is strict
         * @see #isStrict()
         */
        public void setStrict(boolean strict)
        {
            this.strict = strict;
        }
    }

    private static class SignatureContextInformation
        implements IContextInformation
    {
        final SignatureHelp signatureHelp;
        final int signatureIndex;
        final String label;

        SignatureContextInformation(SignatureHelp signatureHelp, int signatureIndex)
        {
            this.signatureHelp = signatureHelp;
            this.signatureIndex = signatureIndex;
            label = signatureHelp.getSignatures().get(signatureIndex).getLabel();
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
        private Integer markedParameter;
        private MarkSignatureActiveParameterOptions markActiveParameterOptions;

        @Override
        public void install(IContextInformation info, ITextViewer viewer, int offset)
        {
            this.info = (SignatureContextInformation)info;
            this.viewer = viewer;
            signatureHelp = null;
            markedParameter = null;
            SignatureInformation signature =
                this.info.signatureHelp.getSignatures().get(this.info.signatureIndex);
            markActiveParameterOptions =
                getParameterCount(signature) > 0 ? getMarkSignatureActiveParameterOptions() : null;
        }

        @Override
        public boolean isContextInformationValid(int offset)
        {
            signatureHelp = computeSignatureHelp(offset);
            if (signatureHelp == null)
                return false;

            return findSignatureByLabel(signatureHelp.getSignatures(), info.label) != null;
        }

        @Override
        public boolean updatePresentation(int offset, TextPresentation presentation)
        {
            if (markActiveParameterOptions == null)
                return false;

            if (signatureHelp == null)
                signatureHelp = computeSignatureHelp(offset);
            if (signatureHelp == null)
                return clearMarkedParameter(presentation);

            SignatureInformation signature =
                findSignatureByLabel(signatureHelp.getSignatures(), info.label);
            if (signature == null)
                return clearMarkedParameter(presentation);

            Integer activeParameter = signature.getActiveParameter();
            if (activeParameter == null)
            {
                if (markActiveParameterOptions.isStrict())
                {
                    SignatureInformation activeSignature =
                        signatureHelp.getSignatures().get(signatureHelp.getActiveSignature());
                    if (!info.label.equals(activeSignature.getLabel()))
                        return clearMarkedParameter(presentation);
                }
                activeParameter = signatureHelp.getActiveParameter();
            }

            if (activeParameter.equals(markedParameter))
                return false;

            if (activeParameter >= getParameterCount(signature))
                return clearMarkedParameter(presentation);

            Two<Integer, Integer> offsets =
                getOffsets(signature.getParameters().get(activeParameter));
            if (offsets == null)
                return clearMarkedParameter(presentation);

            markedParameter = activeParameter;

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

        private boolean clearMarkedParameter(TextPresentation presentation)
        {
            if (markedParameter == null)
                return false;

            markedParameter = null;
            presentation.clear();
            return true;
        }

        private SignatureHelp computeSignatureHelp(int offset)
        {
            SignatureHelpContext context = new SignatureHelpContext();
            context.setTriggerKind(SignatureHelpTriggerKind.ContentChange);
            context.setIsRetrigger(true);
            context.setActiveSignatureHelp(info.signatureHelp);
            info.signatureHelp.setActiveSignature(info.signatureIndex);

            SignatureHelpResult result = computeSignatureHelpResult(viewer, offset, context);
            if (result == null)
                return null;

            SignatureHelp signatureHelp = result.getSignatureHelp();
            if (signatureHelp != null)
                normalize(signatureHelp);

            return signatureHelp;
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
