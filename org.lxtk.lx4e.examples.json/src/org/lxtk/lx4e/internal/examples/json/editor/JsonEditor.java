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
package org.lxtk.lx4e.internal.examples.json.editor;

import static org.lxtk.lx4e.internal.examples.json.JsonPreferenceConstants.EDITOR_MATCHING_BRACKETS;
import static org.lxtk.lx4e.internal.examples.json.JsonPreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm4e.languageconfiguration.LanguageConfigurationCharacterPairMatcher;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.internal.examples.json.Activator;
import org.lxtk.lx4e.internal.examples.json.JsonOperationTargetProvider;
import org.lxtk.lx4e.internal.examples.json.JsonSourceFileDocumentProvider;
import org.lxtk.lx4e.internal.examples.json.JsonSourceViewerConfiguration;
import org.lxtk.lx4e.ui.folding.FoldingManager;

/**
 * JSON-specific text editor.
 */
public class JsonEditor
    extends AbstractDecoratedTextEditor
{
    private IContentOutlinePage outlinePage;
    private ProjectionSupport projectionSupport;
    private FoldingManager foldingManager;

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);

        ProjectionViewer viewer = (ProjectionViewer)getSourceViewer();

        projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
        projectionSupport.install();

        foldingManager = new FoldingManager(viewer, this::getLanguageOperationTarget);
        foldingManager.install();

        viewer.doOperation(ProjectionViewer.TOGGLE);
    }

    @Override
    public void dispose()
    {
        try
        {
            if (foldingManager != null)
            {
                foldingManager.uninstall();
                foldingManager = null;
            }
            if (projectionSupport != null)
            {
                projectionSupport.dispose();
                projectionSupport = null;
            }
        }
        finally
        {
            super.dispose();
        }
    }

    @Override
    protected void initializeEditor()
    {
        IPreferenceStore preferenceStore = new ChainedPreferenceStore(new IPreferenceStore[] {
            Activator.getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() });
        setPreferenceStore(preferenceStore);

        JsonSourceFileDocumentProvider documentProvider =
            Activator.getDefault().getDocumentProvider();
        setDocumentProvider(documentProvider);
        setSourceViewerConfiguration(
            new JsonSourceViewerConfiguration(preferenceStore, this, documentProvider));
        setEditorContextMenuId("#ExampleJsonEditorContext"); //$NON-NLS-1$
        setRulerContextMenuId("#ExampleJsonRulerContext"); //$NON-NLS-1$
    }

    @Override
    protected void initializeKeyBindingScopes()
    {
        setKeyBindingScopes(new String[] { "org.lxtk.lx4e.examples.json.editor.scope" }); //$NON-NLS-1$
    }

    @Override
    protected void configureSourceViewerDecorationSupport(
        SourceViewerDecorationSupport decorationSupport)
    {
        decorationSupport.setCharacterPairMatcher(new LanguageConfigurationCharacterPairMatcher());
        decorationSupport.setMatchingCharacterPainterPreferenceKeys(EDITOR_MATCHING_BRACKETS,
            EDITOR_MATCHING_BRACKETS_COLOR);
        super.configureSourceViewerDecorationSupport(decorationSupport);
    }

    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles)
    {
        ProjectionViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(),
            isOverviewRulerVisible(), styles);
        // ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(viewer);
        return viewer;
    }

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

    void outlinePageClosed()
    {
        if (outlinePage != null)
        {
            outlinePage = null;
            resetHighlightRange();
        }
    }

    private LanguageOperationTarget getLanguageOperationTarget()
    {
        return JsonOperationTargetProvider.getOperationTarget(this);
    }
}
