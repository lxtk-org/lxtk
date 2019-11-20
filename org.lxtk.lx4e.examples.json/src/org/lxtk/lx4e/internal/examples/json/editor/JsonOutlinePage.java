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
package org.lxtk.lx4e.internal.examples.json.editor;

import org.eclipse.handly.model.IElementChangeListener;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.handly.ui.outline.HandlyOutlinePage;
import org.eclipse.handly.ui.outline.ProblemMarkerListenerContribution;
import org.eclipse.handly.ui.preference.BooleanPreference;
import org.eclipse.handly.ui.preference.FlushingPreferenceStore;
import org.eclipse.handly.ui.preference.IBooleanPreference;
import org.eclipse.handly.ui.viewer.ElementTreeContentProvider;
import org.eclipse.handly.ui.viewer.ProblemMarkerLabelDecorator;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IEditorPart;
import org.lxtk.lx4e.examples.json.JsonCore;
import org.lxtk.lx4e.examples.json.JsonInputElementProvider;
import org.lxtk.lx4e.internal.examples.json.Activator;
import org.lxtk.lx4e.ui.LanguageElementLabelProvider;

/**
 * TODO JavaDoc
 */
public class JsonOutlinePage
    extends HandlyOutlinePage
{
    /**
     * TODO JavaDoc
     *
     * @param editor not <code>null</code>
     */
    public JsonOutlinePage(IEditorPart editor)
    {
        init(editor);
    }

    @Override
    public void dispose()
    {
        IEditorPart editor = getEditor();
        if (editor instanceof JsonEditor)
            ((JsonEditor)editor).outlinePageClosed();
        super.dispose();
    }

    @Override
    public IBooleanPreference getLinkWithEditorPreference()
    {
        return LinkWithEditorPreference.INSTANCE;
    }

    @Override
    public IBooleanPreference getLexicalSortPreference()
    {
        return null;
    }

    @Override
    protected void addOutlineContributions()
    {
        super.addOutlineContributions();
        addOutlineContribution(new ProblemMarkerListenerContribution());
    }

    @Override
    protected IInputElementProvider getInputElementProvider()
    {
        return JsonInputElementProvider.INSTANCE;
    }

    @Override
    protected void addElementChangeListener(IElementChangeListener listener)
    {
        JsonCore.addElementChangeListener(listener);
    }

    @Override
    protected void removeElementChangeListener(IElementChangeListener listener)
    {
        JsonCore.removeElementChangeListener(listener);
    }

    @Override
    protected ITreeContentProvider getContentProvider()
    {
        return new ElementTreeContentProvider();
    }

    @Override
    protected IBaseLabelProvider getLabelProvider()
    {
        return new DecoratingStyledCellLabelProvider(
            new LanguageElementLabelProvider(),
            new ProblemMarkerLabelDecorator(), null);
    }

    private static class LinkWithEditorPreference
        extends BooleanPreference
    {
        static final LinkWithEditorPreference INSTANCE =
            new LinkWithEditorPreference();

        LinkWithEditorPreference()
        {
            super("JsonOutline.LinkWithEditor", new FlushingPreferenceStore( //$NON-NLS-1$
                Activator.getDefault().getPreferenceStore()));
            setDefault(true);
        }
    }
}
