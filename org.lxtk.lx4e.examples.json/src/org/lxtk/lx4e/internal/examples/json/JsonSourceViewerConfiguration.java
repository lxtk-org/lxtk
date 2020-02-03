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
 *     Alexander Kozinko (1C) - TM4E-based syntax highlighting
 *******************************************************************************/
package org.lxtk.lx4e.internal.examples.json;

import java.net.URI;

import org.eclipse.handly.ui.IWorkingCopyManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.tm4e.languageconfiguration.LanguageConfigurationAutoEditStrategy;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.examples.json.JsonCore;
import org.lxtk.lx4e.examples.json.JsonInputElementProvider;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.ui.completion.CompletionProposalSorter;
import org.lxtk.lx4e.ui.completion.ContentAssistProcessor;
import org.lxtk.lx4e.ui.hover.TextHover;

/**
 * TODO JavaDoc
 */
public class JsonSourceViewerConfiguration
    extends TextSourceViewerConfiguration
{
    private final ITextEditor editor;
    private final IWorkingCopyManager workingCopyManager;

    public JsonSourceViewerConfiguration(IPreferenceStore preferenceStore,
        ITextEditor editor, IWorkingCopyManager workingCopyManager)
    {
        super(preferenceStore);
        this.editor = editor;
        this.workingCopyManager = workingCopyManager;
    }

    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer,
        String contentType)
    {
        return new IAutoEditStrategy[] {
            new LanguageConfigurationAutoEditStrategy() };
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(
        ISourceViewer viewer)
    {
        return new TMPresentationReconciler();
    }

    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer)
    {
        if (editor == null || !editor.isEditable()
            || workingCopyManager == null)
            return null;

        return new JsonReconciler(editor, workingCopyManager);
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
    {
        if (editor == null || !editor.isEditable())
            return null;

        ContentAssistant assistant = new ContentAssistant(true);
        assistant.setContentAssistProcessor(new ContentAssistProcessor(
            this::getLanguageOperationTarget), IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setSorter(new CompletionProposalSorter());
        assistant.setInformationControlCreator(
            parent -> new DefaultInformationControl(parent, true));
        assistant.enableColoredLabels(true);
        return assistant;
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer,
        String contentType)
    {
        return new TextHover(this::getLanguageOperationTarget);
    }

    private LanguageOperationTarget getLanguageOperationTarget()
    {
        if (editor == null)
            return null;

        ILanguageSourceFile sourceFile =
            JsonInputElementProvider.INSTANCE.getElement(
                editor.getEditorInput());
        if (sourceFile == null)
            return null;

        URI documentUri = sourceFile.getDocumentUri();
        if (documentUri == null)
            return null;

        return new LanguageOperationTarget(documentUri, JsonCore.LANG_ID,
            JsonCore.LANG_SERVICE);
    }
}
