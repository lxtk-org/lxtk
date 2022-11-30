/*******************************************************************************
 * Copyright (c) 2020, 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.quickassist.IQuickFixableAnnotation;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.eclipse.ui.views.markers.MarkerViewUtil;

/**
 * An action which gets triggered when an annotation is selected in the vertical ruler.
 */
public class SelectAnnotationRulerAction
    extends Action
    implements IUpdate
{
    private final ITextEditor editor;
    private final IVerticalRulerInfo rulerInfo;
    private final IPreferenceStore preferenceStore;
    private final AnnotationPreferenceLookup annotationPreferenceLookup =
        EditorsUI.getAnnotationPreferenceLookup();

    /**
     * Constructor.
     *
     * @param editor not <code>null</code>
     * @param rulerInfo not <code>null</code>
     * @param preferenceStore not <code>null</code>
     */
    public SelectAnnotationRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo,
        IPreferenceStore preferenceStore)
    {
        this.editor = Objects.requireNonNull(editor);
        this.rulerInfo = Objects.requireNonNull(rulerInfo);
        this.preferenceStore = Objects.requireNonNull(preferenceStore);
    }

    /**
     * Returns the action's editor.
     *
     * @return the action's editor (never <code>null</code>)
     */
    public final ITextEditor getEditor()
    {
        return editor;
    }

    /**
     * Returns the action's ruler info.
     *
     * @return the action's ruler info (never <code>null</code>)
     */
    public final IVerticalRulerInfo getRulerInfo()
    {
        return rulerInfo;
    }

    /**
     * Returns the action's preference store.
     *
     * @return the action's preference store (never <code>null</code>)
     */
    public final IPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    @Override
    public void update()
    {
        setEnabled(hasAnnotations());
    }

    @Override
    public void run()
    {
        Annotation annotation = chooseAnnotation(getAnnotations());
        if (annotation != null)
            run(annotation);
    }

    /**
     * Runs this action, passing the selected annotation.
     *
     * @param annotation never <code>null</code>
     */
    protected void run(Annotation annotation)
    {
        IAnnotationModel model = getAnnotationModel();
        if (model == null)
            return;

        Position position = model.getPosition(annotation);
        if (position == null || position.isDeleted())
            return;

        editor.selectAndReveal(position.getOffset(), position.getLength());

        IMarker marker = getMarker(annotation);
        if (marker != null)
            MarkerViewUtil.showMarker(editor.getSite().getPage(), marker, false);

        if (annotation instanceof IQuickFixableAnnotation)
        {
            IQuickFixableAnnotation quickFixableAnnotation = (IQuickFixableAnnotation)annotation;
            if (quickFixableAnnotation.isQuickFixableStateSet()
                && quickFixableAnnotation.isQuickFixable())
            {
                ITextOperationTarget operation = editor.getAdapter(ITextOperationTarget.class);
                if (operation != null && operation.canDoOperation(ISourceViewer.QUICK_ASSIST))
                {
                    operation.doOperation(ISourceViewer.QUICK_ASSIST);
                }
            }
        }
    }

    /**
     * Chooses the annotation with the highest layer. If there are multiple annotations
     * at the found layer, the first one is taken.
     *
     * @param annotations the collection of annotations to choose from (not <code>null</code>,
     *  may be empty)
     * @return the chosen annotation, or <code>null</code> if the given collection is empty
     */
    protected final Annotation chooseAnnotation(Collection<? extends Annotation> annotations)
    {
        Annotation result = null;
        int maxLayer = 0;
        IAnnotationAccessExtension access = getAnnotationAccessExtension();

        for (Annotation annotation : annotations)
        {
            if (access == null)
            {
                result = annotation;
                break;
            }
            int layer = access.getLayer(annotation);
            if (layer == maxLayer)
            {
                if (result == null)
                    result = annotation;
            }
            else if (layer > maxLayer)
            {
                maxLayer = layer;
                result = annotation;
            }
        }

        return result;
    }

    /**
     * Returns all annotations which include the ruler's line of activity.
     *
     * @return a collection of all annotations which include the ruler's line of activity
     *  (never <code>null</code>, may be empty)
     */
    protected final Collection<Annotation> getAnnotations()
    {
        IDocument document = getDocument();
        if (document == null)
            return Collections.emptyList();

        IAnnotationModel model = getAnnotationModel();
        if (model == null)
            return Collections.emptyList();

        int activeLine = rulerInfo.getLineOfLastMouseButtonActivity();
        if (activeLine < 0)
            return Collections.emptyList();

        Iterator<Annotation> it;
        if (model instanceof IAnnotationModelExtension2)
        {
            try
            {
                IRegion line = document.getLineInformation(activeLine);
                it = ((IAnnotationModelExtension2)model).getAnnotationIterator(line.getOffset(),
                    line.getLength() + 1, true, true);
            }
            catch (BadLocationException e)
            {
                it = model.getAnnotationIterator();
            }
        }
        else
            it = model.getAnnotationIterator();

        Collection<Annotation> result = new ArrayList<>();
        while (it.hasNext())
        {
            Annotation annotation = it.next();
            if (isIncluded(annotation) && isVisible(annotation)
                && includesLine(model.getPosition(annotation), document, activeLine))
            {
                result.add(annotation);
            }
        }
        return result;
    }

    /**
     * Returns <code>true</code> iff there are any annotations which include the ruler's
     * line of activity.
     *
     * @return <code>true</code> iff there are any annotations which include the ruler's
     * line of activity
     */
    protected final boolean hasAnnotations()
    {
        IDocument document = getDocument();
        if (document == null)
            return false;

        IAnnotationModel model = getAnnotationModel();
        if (model == null)
            return false;

        int activeLine = rulerInfo.getLineOfLastMouseButtonActivity();
        if (activeLine < 0)
            return false;

        Iterator<Annotation> it;
        if (model instanceof IAnnotationModelExtension2)
        {
            try
            {
                IRegion line = document.getLineInformation(activeLine);
                it = ((IAnnotationModelExtension2)model).getAnnotationIterator(line.getOffset(),
                    line.getLength() + 1, true, true);
            }
            catch (BadLocationException e)
            {
                it = model.getAnnotationIterator();
            }
        }
        else
            it = model.getAnnotationIterator();

        while (it.hasNext())
        {
            Annotation annotation = it.next();
            if (isIncluded(annotation) && isVisible(annotation)
                && includesLine(model.getPosition(annotation), document, activeLine))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether the given annotation should be included in the computation.
     *
     * @param annotation the annotation to test (never <code>null</code>)
     * @return <code>true</code> if the annotation is included in the computation,
     *  and <code>false</code> otherwise
     */
    protected boolean isIncluded(Annotation annotation)
    {
        return true;
    }

    /**
     * Returns the annotation access extension.
     *
     * @return the annotation access extension, or <code>null</code>
     *  if this action's editor has no such extension
     */
    protected final IAnnotationAccessExtension getAnnotationAccessExtension()
    {
        Object adapter = editor.getAdapter(IAnnotationAccess.class);
        if (adapter instanceof IAnnotationAccessExtension)
            return (IAnnotationAccessExtension)adapter;
        return null;
    }

    /**
     * Returns the annotation model of the editor's input.
     *
     * @return the annotation model, or <code>null</code> if there's none
     */
    protected final IAnnotationModel getAnnotationModel()
    {
        IDocumentProvider provider = editor.getDocumentProvider();
        if (provider == null)
            return null;
        return provider.getAnnotationModel(editor.getEditorInput());
    }

    /**
     * Returns the <code>IDocument</code> of the editor's input.
     *
     * @return the document of the editor's input, or <code>null</code> if none
     */
    protected final IDocument getDocument()
    {
        IDocumentProvider provider = editor.getDocumentProvider();
        if (provider == null)
            return null;
        return provider.getDocument(editor.getEditorInput());
    }

    /**
     * Checks whether a position includes the the given line.
     *
     * @param position the position to be checked (may be <code>null</code>)
     * @param document the document the position refers to (not <code>null</code>)
     * @param line 0-based
     * @return <code>true</code> if the line is included by the given position
     */
    protected boolean includesLine(Position position, IDocument document, int line)
    {
        if (position != null)
        {
            try
            {
                if (line == document.getLineOfOffset(position.getOffset()))
                    return true;
            }
            catch (BadLocationException x)
            {
            }
        }
        return false;
    }

    /**
     * Returns the marker that corresponds to the given annotation.
     *
     * @param annotation never <code>null</code>
     * @return the corresponding marker, or <code>null</code> if none
     */
    protected IMarker getMarker(Annotation annotation)
    {
        if (annotation instanceof SimpleMarkerAnnotation)
            return ((SimpleMarkerAnnotation)annotation).getMarker();

        IAnnotationModel model = getAnnotationModel();
        if (model == null)
            return null;

        Position position = model.getPosition(annotation);
        if (position == null || position.isDeleted())
            return null;

        List<Annotation> annotations = new ArrayList<>();
        Iterator<Annotation> it =
            !(model instanceof IAnnotationModelExtension2) ? model.getAnnotationIterator()
                : ((IAnnotationModelExtension2)model).getAnnotationIterator(position.getOffset(),
                    position.getLength(), false, false);
        while (it.hasNext())
        {
            Annotation a = it.next();
            if (a instanceof SimpleMarkerAnnotation)
            {
                Position p = model.getPosition(a);
                if (position.equals(p) && !p.isDeleted())
                    annotations.add(a);
            }
        }
        Annotation a = chooseAnnotation(annotations);
        if (a instanceof SimpleMarkerAnnotation)
            return ((SimpleMarkerAnnotation)a).getMarker();
        return null;
    }

    private boolean isVisible(Annotation annotation)
    {
        AnnotationPreference preference =
            annotationPreferenceLookup.getAnnotationPreference(annotation);
        if (preference == null)
            return false;

        String key = preference.getVerticalRulerPreferenceKey();
        if (key == null)
            return false;

        return preferenceStore.getBoolean(key);
    }
}
