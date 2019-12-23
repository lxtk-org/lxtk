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
 *     Alexander Kozinko (1C) - TM4E-based syntax highlight
 *******************************************************************************/
package org.lxtk.lx4e.internal.examples.json.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.tm4e.languageconfiguration.LanguageConfigurationCharacterPairMatcher;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.lxtk.lx4e.internal.examples.json.Activator;
import org.lxtk.lx4e.internal.examples.json.JsonSourceFileDocumentProvider;
import org.lxtk.lx4e.internal.examples.json.JsonSourceViewerConfiguration;

/**
 * TODO JavaDoc
 */
public class JsonEditor
    extends AbstractDecoratedTextEditor
{
    private static final String ENCLOSING_BRACKETS = "enclosingBrackets"; //$NON-NLS-1$
    private static final String HIGHLIGHT_BRACKET_AT_CARET_LOCATION = "highlightBracketAtCaretLocation"; //$NON-NLS-1$
    private static final String MATCHING_BRACKETS_COLOR = "matchingBracketsColor"; //$NON-NLS-1$
    private static final String MATCHING_BRACKETS = "matchingBrackets"; //$NON-NLS-1$

    private IContentOutlinePage outlinePage;

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == IContentOutlinePage.class)
        {
            if (outlinePage == null)
                outlinePage = new JsonOutlinePage(this);
            return adapter.cast(outlinePage);
        }
        return super.getAdapter(adapter);
    }

    /**
     * Informs the editor that its outline page has been closed.
     */
    public void outlinePageClosed()
    {
        if (outlinePage != null)
        {
            outlinePage = null;
            resetHighlightRange();
        }
    }

    @Override
    protected void initializeEditor()
    {
        super.initializeEditor();
        JsonSourceFileDocumentProvider documentProvider = Activator.getDefault().getDocumentProvider();
        setDocumentProvider(documentProvider);
        setSourceViewerConfiguration(new JsonSourceViewerConfiguration(getPreferenceStore(), this, documentProvider));

        IPreferenceStore store = getPreferenceStore();
        store.setDefault(MATCHING_BRACKETS, true);
        store.setDefault(MATCHING_BRACKETS_COLOR, "128,128,128"); //$NON-NLS-1$
    }

    @Override
    protected void initializeKeyBindingScopes()
    {
        setKeyBindingScopes(new String[] { "org.lxtk.lx4e.examples.json.editor.scope" }); //$NON-NLS-1$
    }

    @Override
    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport decorationSupport)
    {
        decorationSupport.setCharacterPairMatcher(new LanguageConfigurationCharacterPairMatcher());
        decorationSupport.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR,
            HIGHLIGHT_BRACKET_AT_CARET_LOCATION, ENCLOSING_BRACKETS);
        super.configureSourceViewerDecorationSupport(decorationSupport);
    }
}
