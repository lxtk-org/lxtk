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
package org.lxtk.lx4e.ui.hover;

import java.net.URI;
import java.time.Duration;
import java.util.Iterator;
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
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.DocumentUri;
import org.lxtk.HoverProvider;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.FocusableInformationControlCreator;
import org.lxtk.lx4e.internal.ui.StyledBrowserInformationControlInput;
import org.lxtk.lx4e.util.DefaultWordFinder;
import org.lxtk.lx4e.util.Markdown;

/**
 * TODO JavaDoc
 */
public class TextHover
    implements ITextHover, ITextHoverExtension, ITextHoverExtension2
{
    private final Supplier<LanguageOperationTarget> targetSupplier;
    private IInformationControlCreator hoverControlCreator;

    /**
     * TODO JavaDoc
     *
     * @param targetSupplier not <code>null</code>
     */
    public TextHover(Supplier<LanguageOperationTarget> targetSupplier)
    {
        this.targetSupplier = Objects.requireNonNull(targetSupplier);
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset)
    {
        return findWord(textViewer.getDocument(), offset);
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
        LanguageOperationTarget target = targetSupplier.get();
        if (target == null)
            return null;
        URI documentUri = target.getDocumentUri();
        LanguageService languageService = target.getLanguageService();
        HoverProvider provider =
            languageService.getDocumentMatcher().getBestMatch(
                languageService.getHoverProviders(),
                HoverProvider::getDocumentSelector, documentUri,
                target.getLanguageId());
        if (provider == null)
            return null;
        IDocument document = textViewer.getDocument();
        Position position;
        try
        {
            position = DocumentUtil.toPosition(document,
                hoverRegion.getOffset());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }
        CompletableFuture<Hover> future = provider.getHover(
            new TextDocumentPositionParams(DocumentUri.toTextDocumentIdentifier(
                documentUri), position));
        Hover result = null;
        try
        {
            result = future.get(getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (CancellationException | InterruptedException e)
        {
        }
        catch (ExecutionException e)
        {
            Activator.logError(e);
        }
        catch (TimeoutException e)
        {
            Activator.logWarning(e);
        }
        if (result == null)
            return null;
        Either<List<Either<String, MarkedString>>, MarkupContent> contents =
            result.getContents();
        MarkupContent markupContent;
        if (contents.isLeft())
        {
            StringBuilder builder = new StringBuilder();
            Iterator<Either<String, MarkedString>> it =
                contents.getLeft().iterator();
            while (it.hasNext())
            {
                Either<String, MarkedString> item = it.next();
                if (item.isLeft())
                {
                    builder.append(item.getLeft());
                    if (it.hasNext() && !item.getLeft().isEmpty())
                        builder.append("\n\n"); //$NON-NLS-1$
                }
                else if (item.isRight())
                {
                    MarkedString markedString = item.getRight();
                    builder.append("```"); //$NON-NLS-1$
                    builder.append(markedString.getLanguage());
                    builder.append('\n');
                    builder.append(markedString.getValue());
                    builder.append("\n```"); //$NON-NLS-1$
                    if (it.hasNext())
                        builder.append("\n\n"); //$NON-NLS-1$
                }
            }
            markupContent = new MarkupContent(MarkupKind.MARKDOWN,
                builder.toString());
        }
        else
            markupContent = contents.getRight();
        if (markupContent == null)
            return null;
        return toHoverInfo(markupContent);
    }

    /**
     * TODO JavaDoc
     *
     * @param markupContent never <code>null</code>
     * @return the hover popup display information, or <code>null</code> if none
     */
    protected Object toHoverInfo(MarkupContent markupContent)
    {
        String value = markupContent.getValue();
        if (value.isEmpty())
            return null;
        String html = Markdown.toHtml(value, false);
        return StyledBrowserInformationControlInput.of(html);
    }

    @Override
    public IInformationControlCreator getHoverControlCreator()
    {
        if (hoverControlCreator == null)
            hoverControlCreator = newHoverControlCreator();
        return hoverControlCreator;
    }

    /**
     * TODO JavaDoc
     *
     * @return a new hover control creator (not <code>null</code>)
     */
    protected IInformationControlCreator newHoverControlCreator()
    {
        return new FocusableInformationControlCreator();
    }

    /**
     * TODO JavaDoc
     *
     * @param document never <code>null</code>
     * @param offset 0-based
     * @return the corresponding word region, or <code>null</code> if none
     */
    protected IRegion findWord(IDocument document, int offset)
    {
        return DefaultWordFinder.INSTANCE.findWord(document, offset);
    }

    /**
     * TODO JavaDoc
     *
     * @return a positive duration
     */
    protected Duration getTimeout()
    {
        return Duration.ofSeconds(2);
    }
}