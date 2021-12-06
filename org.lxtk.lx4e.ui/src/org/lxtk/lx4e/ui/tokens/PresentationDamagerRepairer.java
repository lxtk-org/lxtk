/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.tokens;

import java.net.URI;
import java.time.Duration;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensRangeParams;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.lxtk.DocumentSemanticTokensProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.requests.DocumentRangeSemanticTokensRequest;

/**
 * Base implementation of an {@link IPresentationRepairer} that computes the "repair description"
 * using a {@link DocumentSemanticTokensProvider}. Also implements {@link IPresentationDamager}.
 */
public abstract class PresentationDamagerRepairer
    implements IPresentationDamager, IPresentationRepairer
{
    private final Supplier<LanguageOperationTarget> targetSupplier;
    private final TextAttribute defaultTextAttribute = new TextAttribute(null);
    @SuppressWarnings("deprecation")
    private final IPresentationDamager damager =
        new DefaultDamagerRepairer(null, defaultTextAttribute);
    private IDocument document;

    /**
     * Constructor.
     *
     * @param targetSupplier a {@link LanguageOperationTarget} supplier (not <code>null</code>)
     */
    public PresentationDamagerRepairer(Supplier<LanguageOperationTarget> targetSupplier)
    {
        this.targetSupplier = Objects.requireNonNull(targetSupplier);
    }

    @Override
    public void setDocument(IDocument document)
    {
        this.document = document;
        damager.setDocument(document);
    }

    @Override
    public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent event,
        boolean documentPartitioningChanged)
    {
        return damager.getDamageRegion(partition, event, documentPartitioningChanged);
    }

    @Override
    public final void createPresentation(TextPresentation presentation, ITypedRegion damage)
    {
        LanguageOperationTarget target = targetSupplier.get();
        if (target != null)
        {
            URI documentUri = target.getDocumentUri();
            LanguageService languageService = target.getLanguageService();

            DocumentSemanticTokensProvider provider =
                languageService.getDocumentMatcher().getBestMatch(
                    languageService.getDocumentSemanticTokensProviders(),
                    DocumentSemanticTokensProvider::getDocumentSelector, documentUri,
                    target.getLanguageId());
            if (provider != null)
            {
                Range range = null;
                try
                {
                    range = DocumentUtil.toRange(document, damage.getOffset(), damage.getLength());
                }
                catch (BadLocationException e)
                {
                    Activator.logError(e);
                }
                if (range != null)
                {
                    DocumentRangeSemanticTokensRequest request =
                        newDocumentRangeSemanticTokensRequest();
                    request.setProvider(provider);
                    request.setParams(new SemanticTokensRangeParams(
                        DocumentUri.toTextDocumentIdentifier(documentUri), range));
                    request.setTimeout(getSemanticTokensRequestTimeout());
                    request.setMayThrow(false);

                    SemanticTokens tokens = request.sendAndReceive();
                    if (tokens != null)
                    {
                        List<Integer> data = tokens.getData();
                        if (data.size() % 5 == 0)
                        {
                            createPresentation(presentation, damage, data,
                                provider.getRegistrationOptions().getLegend());
                            return;
                        }
                    }
                }
            }
        }
        // fallback
        addStyleRange(presentation, damage.getOffset(), damage.getLength(), defaultTextAttribute);
    }

    /**
     * Returns the text attribute for the given semantic token.
     *
     * @param token never <code>null</code>
     * @return the corresponding text attribute, or <code>null</code> if there is none
     */
    protected abstract TextAttribute getTokenTextAttribute(Token token);

    /**
     * Returns the default text attribute.
     *
     * @return the default text attribute (never <code>null</code>)
     */
    protected final TextAttribute getDefaultTextAttribute()
    {
        return defaultTextAttribute;
    }

    /**
     * Returns a new instance of {@link DocumentRangeSemanticTokensRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentRangeSemanticTokensRequest newDocumentRangeSemanticTokensRequest()
    {
        return new DocumentRangeSemanticTokensRequest();
    }

    /**
     * Returns the timeout for a semantic tokens request.
     *
     * @return a positive duration
     */
    protected Duration getSemanticTokensRequestTimeout()
    {
        return Duration.ofMillis(100);
    }

    private void createPresentation(TextPresentation presentation, IRegion damage,
        List<Integer> data, SemanticTokensLegend legend)
    {
        int startOffset = damage.getOffset();
        int endOffset = damage.getOffset() + damage.getLength();
        for (int i = 0, line = 0, character = 0, size = data.size(); i < size; i += 5)
        {
            Integer deltaLine = data.get(i);
            if (deltaLine == null || deltaLine < 0)
                break;
            line += deltaLine;

            Integer deltaCharacter = data.get(i + 1);
            if (deltaCharacter == null || deltaCharacter < 0)
                break;
            if (deltaLine > 0)
                character = 0;
            character += deltaCharacter;

            int offset;
            try
            {
                offset = DocumentUtil.toOffset(document, new Position(line, character));
            }
            catch (BadLocationException e)
            {
                break;
            }

            if (offset >= endOffset)
                break;

            Integer length = data.get(i + 2);
            if (length == null || length <= 0)
                continue;

            if (offset + length <= startOffset)
                continue;

            Integer typeIndex = data.get(i + 3);
            if (typeIndex == null || typeIndex < 0 || typeIndex >= legend.getTokenTypes().size())
                continue;

            String type = legend.getTokenTypes().get(typeIndex);

            Integer modifierFlags = data.get(i + 4);
            if (modifierFlags == null)
                continue;

            Set<String> modifiers = getTokenModifiers(modifierFlags, legend.getTokenModifiers());

            TextAttribute tokenTextAttribute = getTokenTextAttribute(new Token(type, modifiers));
            if (tokenTextAttribute == null)
                continue;

            if (offset > startOffset)
                addStyleRange(presentation, startOffset, offset - startOffset,
                    defaultTextAttribute);

            addStyleRange(presentation, offset, length, tokenTextAttribute);

            startOffset = offset + length;
        }
        if (startOffset < endOffset)
            addStyleRange(presentation, startOffset, endOffset - startOffset, defaultTextAttribute);
    }

    private static Set<String> getTokenModifiers(int modifierFlags, List<String> allModifiers)
    {
        Set<String> result = new HashSet<>();
        BitSet bs = BitSet.valueOf(new long[] { modifierFlags });
        for (int i = bs.nextSetBit(0), size = allModifiers.size(); i >= 0 && i < size;
            i = bs.nextSetBit(i + 1))
        {
            result.add(allModifiers.get(i));
        }
        return result;
    }

    private static void addStyleRange(TextPresentation presentation, int offset, int length,
        TextAttribute attr)
    {
        int style = attr.getStyle();
        int fontStyle = style & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL);
        StyleRange styleRange =
            new StyleRange(offset, length, attr.getForeground(), attr.getBackground(), fontStyle);
        styleRange.strikeout = (style & TextAttribute.STRIKETHROUGH) != 0;
        styleRange.underline = (style & TextAttribute.UNDERLINE) != 0;
        styleRange.font = attr.getFont();
        presentation.addStyleRange(styleRange);
    }

    /**
     * Represents a semantic token.
     */
    protected final static class Token
    {
        private final String type;
        private final Set<String> modifiers;

        Token(String type, Set<String> modifiers)
        {
            this.type = Objects.requireNonNull(type);
            this.modifiers = Objects.requireNonNull(modifiers);
        }

        /**
         * Returns the type of this token.
         *
         * @return the token type (never <code>null</code>)
         * @see org.eclipse.lsp4j.SemanticTokenTypes
         */
        public String getType()
        {
            return type;
        }

        /**
         * Returns the set of modifiers for this token.
         *
         * @return the set of token modifiers (never <code>null</code>, may be empty)
         * @see org.eclipse.lsp4j.SemanticTokenModifiers
         */
        public Set<String> getModifiers()
        {
            return modifiers;
        }
    }
}
