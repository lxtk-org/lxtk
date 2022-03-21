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
package org.lxtk.lx4e.ui.hover;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentUri;
import org.lxtk.HoverProvider;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.FocusableInformationControlCreator;
import org.lxtk.lx4e.internal.ui.StyledBrowserInformationControlInput;
import org.lxtk.lx4e.internal.ui.TaskExecutor;
import org.lxtk.lx4e.requests.HoverRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;
import org.lxtk.lx4e.util.DefaultWordFinder;
import org.lxtk.lx4e.util.Markdown;

/**
 * Default implementation of a text hover that computes hover information using
 * {@link HoverProvider}s.
 */
public class DocumentHover
    implements ITextHover, ITextHoverExtension, ITextHoverExtension2
{
    private final Supplier<LanguageOperationTarget> targetSupplier;
    private IInformationControlCreator hoverControlCreator;

    /**
     * Constructor.
     *
     * @param targetSupplier the {@link LanguageOperationTarget} supplier
     *  for this hover (not <code>null</code>)
     */
    public DocumentHover(Supplier<LanguageOperationTarget> targetSupplier)
    {
        this.targetSupplier = Objects.requireNonNull(targetSupplier);
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset)
    {
        return DefaultWordFinder.INSTANCE.findWord(textViewer.getDocument(), offset);
    }

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion)
    {
        Object hoverInfo = getHoverInfo2(textViewer, hoverRegion);
        if (hoverInfo == null)
            return null;

        return hoverInfo.toString();
    }

    @Override
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion)
    {
        MarkupContent markupContent = computeHoverMarkupContent(textViewer, hoverRegion);
        if (markupContent == null)
            return null;

        return toHoverInfo(markupContent);
    }

    @Override
    public IInformationControlCreator getHoverControlCreator()
    {
        if (hoverControlCreator == null)
            hoverControlCreator = newHoverControlCreator();
        return hoverControlCreator;
    }

    /**
     * Returns a new instance of the hover control creator.
     *
     * @return a new hover control creator (not <code>null</code>)
     */
    protected IInformationControlCreator newHoverControlCreator()
    {
        return new FocusableInformationControlCreator();
    }

    /**
     * Returns a new instance of {@link HoverRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected HoverRequest newHoverRequest()
    {
        return new HoverRequest();
    }

    /**
     * Returns the timeout for a hover request.
     *
     * @return a positive duration
     */
    protected Duration getHoverTimeout()
    {
        return Duration.ofSeconds(2);
    }

    /**
     * Returns the hover providers that match the given target.
     *
     * @param target never <code>null</code>
     * @return the matching hover providers (not <code>null</code>)
     */
    protected HoverProvider[] getHoverProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getHoverProviders(), HoverProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(HoverProvider[]::new);
    }

    /**
     * Computes the hover results for the given {@link HoverParams}
     * using the given hover providers.
     *
     * @param providers never <code>null</code>
     * @param params never <code>null</code>
     * @return the computed hover results, or <code>null</code> if none
     */
    protected HoverResults computeHoverResults(HoverProvider[] providers, HoverParams params)
    {
        if (providers.length == 0)
            return null;

        return new HoverResults(TaskExecutor.sequentialCompute(providers,
            (provider, timeout) -> computeHoverResult(provider, params, timeout),
            result -> result != null && result.getHover() != null, getHoverTimeout()));
    }

    /**
     * Computes a hover result for the given {@link HoverParams}
     * using the given hover provider.
     *
     * @param provider never <code>null</code>
     * @param params never <code>null</code>
     * @param timeout a positive duration or <code>null</code>
     * @return the computed hover result, or <code>null</code> if none
     */
    protected HoverResult computeHoverResult(HoverProvider provider, HoverParams params,
        Duration timeout)
    {
        HoverRequest request = newHoverRequest();
        request.setProvider(provider);
        // note that request params can get modified as part of request processing
        // (e.g. a progress token can be set); therefore we need to copy the given params
        request.setParams(JsonUtil.deepCopy(params));
        request.setTimeout(timeout);
        request.setMayThrow(false);
        request.setUpWorkDoneProgress(
            () -> WorkDoneProgressFactory.newWorkDoneProgressWithJob(false));
        return new HoverResult(request.sendAndReceive());
    }

    /**
     * Returns the markup content for the given hover results.
     *
     * @param results never <code>null</code>
     * @return the markup content, or <code>null</code> if none
     */
    protected MarkupContent getMarkupContent(HoverResults results)
    {
        for (Map.Entry<HoverProvider, HoverResult> entry : results.asMap().entrySet())
        {
            HoverResult result = entry.getValue();
            if (result != null)
            {
                Hover hover = result.getHover();
                if (hover != null)
                    return getMarkupContent(hover);
            }
        }
        return null;
    }

    /**
     * Converts the given {@link MarkupContent} to a hover information object.
     *
     * @param markupContent never <code>null</code>
     * @return the corresponding hover information, or <code>null</code> if none
     */
    protected Object toHoverInfo(MarkupContent markupContent)
    {
        String value = markupContent.getValue();
        if (value.isEmpty())
            return null;

        String html = Markdown.toHtml(value, false);
        return StyledBrowserInformationControlInput.of(html);
    }

    private MarkupContent computeHoverMarkupContent(ITextViewer textViewer, IRegion hoverRegion)
    {
        LanguageOperationTarget target = targetSupplier.get();
        if (target == null)
            return null;

        HoverProvider[] providers = getHoverProviders(target);
        if (providers.length == 0)
            return null;

        IDocument document = textViewer.getDocument();

        Position position;
        try
        {
            position = DocumentUtil.toPosition(document, hoverRegion.getOffset());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }

        HoverResults results = computeHoverResults(providers, new HoverParams(
            DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()), position));
        if (results == null)
            return null;

        return getMarkupContent(results);
    }

    @SuppressWarnings("deprecation")
    private static MarkupContent getMarkupContent(Hover hover)
    {
        MarkupContent markupContent;
        Either<List<Either<String, org.eclipse.lsp4j.MarkedString>>, MarkupContent> contents =
            hover.getContents();
        if (contents.isLeft())
        {
            StringBuilder builder = new StringBuilder();
            Iterator<Either<String, org.eclipse.lsp4j.MarkedString>> it =
                contents.getLeft().iterator();
            while (it.hasNext())
            {
                Either<String, org.eclipse.lsp4j.MarkedString> item = it.next();
                if (item.isLeft())
                {
                    builder.append(item.getLeft());
                    if (it.hasNext() && !item.getLeft().isEmpty())
                        builder.append("\n\n"); //$NON-NLS-1$
                }
                else if (item.isRight())
                {
                    org.eclipse.lsp4j.MarkedString markedString = item.getRight();
                    builder.append("```"); //$NON-NLS-1$
                    String language = markedString.getLanguage();
                    builder.append(language);
                    builder.append('\n');
                    String value = markedString.getValue();
                    builder.append(value);
                    builder.append("\n```"); //$NON-NLS-1$
                    if (it.hasNext())
                        builder.append("\n\n"); //$NON-NLS-1$
                }
            }
            markupContent = new MarkupContent(MarkupKind.MARKDOWN, builder.toString());
        }
        else
            markupContent = contents.getRight();
        return markupContent;
    }

    /**
     * Represents a group of {@link HoverResult}s.
     */
    protected static class HoverResults
    {
        private Map<HoverProvider, HoverResult> results;

        /**
         * Constructor.
         *
         * @param results not <code>null</code>
         */
        public HoverResults(Map<HoverProvider, HoverResult> results)
        {
            this.results = Objects.requireNonNull(results);
        }

        /**
         * Returns the hover results as a map.
         *
         * @return the hover results as a map (never <code>null</code>)
         */
        public Map<HoverProvider, HoverResult> asMap()
        {
            return results;
        }
    }

    /**
     * Represents the result of a hover request.
     */
    protected static class HoverResult
    {
        private final Hover hover;

        /**
         * Constructor.
         *
         * @param hover may be <code>null</code>
         */
        public HoverResult(Hover hover)
        {
            this.hover = hover;
        }

        /**
         * Returns the hover for this result.
         *
         * @return the hover, or <code>null</code> if none
         */
        public Hover getHover()
        {
            return hover;
        }
    }
}
