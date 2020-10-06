/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.highlight;

import static org.lxtk.lx4e.internal.util.AnnotationUtil.replaceAnnotations;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.snapshot.DocumentSnapshot;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.ui.PlatformUI;
import org.lxtk.DocumentHighlightProvider;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.DocumentHighlightRequest;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;

/**
 * Highlights document ranges computed using a {@link DocumentHighlightProvider}.
 */
public class Highlighter
{
    private final ISourceViewer viewer;
    private final ISelectionProvider selectionProvider;
    private final Supplier<LanguageOperationTarget> targetSupplier;
    private final ISelectionChangedListener listener = e -> scheduleHighlighting(e.getSelection());
    private final Object jobLock = new Object();
    private HighlightingJob job;
    private ISelection forcedSelection;
    private Collection<Annotation> annotations;
    private ISnapshot snapshot;
    private boolean installed;
    private boolean sticky = true;

    /**
     * Constructor.
     *
     * @param viewer the target source viewer for this highlighter
     *  (not <code>null</code>)
     * @param selectionProvider the selection provider for this highlighter
     *  (not <code>null</code>)
     * @param targetSupplier the {@link LanguageOperationTarget} supplier
     *  for this highlighter (not <code>null</code>)
     */
    public Highlighter(ISourceViewer viewer, ISelectionProvider selectionProvider,
        Supplier<LanguageOperationTarget> targetSupplier)
    {
        this.viewer = Objects.requireNonNull(viewer);
        this.selectionProvider = Objects.requireNonNull(selectionProvider);
        this.targetSupplier = Objects.requireNonNull(targetSupplier);
    }

    /**
     * Installs this highlighter by registering the necessary listeners
     * and scheduling a highlighting job for the current selection.
     */
    public final void install()
    {
        if (selectionProvider instanceof IPostSelectionProvider)
            ((IPostSelectionProvider)selectionProvider).addPostSelectionChangedListener(listener);
        else
            selectionProvider.addSelectionChangedListener(listener);

        forcedSelection = selectionProvider.getSelection();
        scheduleHighlighting(forcedSelection);
        installed = true;
    }

    /**
     * Unistalls this highlighter by unregistering all listeners and removing
     * highlighting.
     */
    public final void uninstall()
    {
        if (selectionProvider instanceof IPostSelectionProvider)
            ((IPostSelectionProvider)selectionProvider).removePostSelectionChangedListener(
                listener);
        else
            selectionProvider.removeSelectionChangedListener(listener);

        if (job != null)
        {
            job.cancel();
            job = null;
        }
        snapshot = null;
        updateAnnotations(null);
        installed = false;
    }

    /**
     * Checks whether this highlighter is currently installed.
     *
     * @return <code>true</code> if this highlighter is currently installed,
     *  and <code>false</code> otherwise
     */
    public final boolean isInstalled()
    {
        return installed;
    }

    /**
     * Checks whether this highlighter is currently in <i>sticky mode</i>.
     * In sticky mode, highlighting stays even if there is no valid symbol
     * at the current caret position.
     *
     * @return <code>true</code> if this highlighter is currently in sticky mode,
     *  and <code>false</code> otherwise
     * @see #setSticky(boolean)
     */
    public final boolean isSticky()
    {
        return sticky;
    }

    /**
     * Turns the sticky mode on or off.
     *
     * @param sticky <code>true</code> to turn sticky mode on;
     *  <code>false</code> to turn it off
     * @see #isSticky()
     */
    public final void setSticky(boolean sticky)
    {
        this.sticky = sticky;
    }

    /**
     * Creates and returns an annotation representing the given {@link
     * DocumentHighlight}.
     *
     * @param highlight never <code>null</code>
     * @return the created annotation (not <code>null</code>)
     */
    protected Annotation createAnnotation(DocumentHighlight highlight)
    {
        Annotation annotation = new Annotation(false);
        annotation.setType(toAnnotationType(highlight.getKind()));
        return annotation;
    }

    /**
     * Returns a new instance of {@link DocumentHighlightRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected DocumentHighlightRequest newDocumentHighlightRequest()
    {
        return new DocumentHighlightRequest();
    }

    private void scheduleHighlighting(ISelection selection)
    {
        if (!(selection instanceof ITextSelection) || selection.isEmpty())
            return;
        IDocument document = viewer.getDocument();
        if (document == null)
            return;
        org.eclipse.lsp4j.Position position;
        try
        {
            position = DocumentUtil.toPosition(document, ((ITextSelection)selection).getOffset());
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return;
        }
        LanguageOperationTarget target = targetSupplier.get();
        if (target == null)
            return;
        if (job != null)
            job.cancel();
        job = new HighlightingJob(target, selection, position);
        job.schedule();
    }

    private void updateAnnotations(Collection<? extends DocumentHighlight> highlights)
    {
        if (!installed)
            return;
        IAnnotationModel annotationModel = viewer.getAnnotationModel();
        if (annotationModel == null)
            return;
        if (highlights == null || highlights.isEmpty())
        {
            replaceAnnotations(annotationModel, annotations, null);
            annotations = null;
        }
        else
        {
            Map<Annotation, Position> toAdd = toAnnotations(highlights);
            replaceAnnotations(annotationModel, annotations, toAdd);
            annotations = new ArrayList<>(toAdd.keySet());
        }
    }

    private Map<Annotation, Position> toAnnotations(
        Collection<? extends DocumentHighlight> highlights)
    {
        IDocument document = viewer.getDocument();
        if (document == null)
            return Collections.emptyMap();
        Map<Annotation, Position> result = new IdentityHashMap<>(highlights.size());
        for (DocumentHighlight highlight : highlights)
        {
            IRegion r;
            try
            {
                r = DocumentUtil.toRegion(document, highlight.getRange());
            }
            catch (BadLocationException e)
            {
                // silently ignore: the document might have changed in the meantime
                continue;
            }
            Annotation annotation = createAnnotation(highlight);
            if (annotation.getText() == null)
            {
                try
                {
                    annotation.setText(MessageFormat.format(getAnnotationText(highlight.getKind()),
                        document.get(r.getOffset(), r.getLength())));
                }
                catch (BadLocationException e)
                {
                    Activator.logError(e);
                }
            }
            result.put(annotation, new Position(r.getOffset(), r.getLength()));
        }
        return result;
    }

    private static String toAnnotationType(DocumentHighlightKind kind)
    {
        switch (kind)
        {
        case Read:
            return "org.lxtk.lx4e.ui.highlight.readOccurrence"; //$NON-NLS-1$
        case Write:
            return "org.lxtk.lx4e.ui.highlight.writeOccurrence"; //$NON-NLS-1$
        default:
            return "org.lxtk.lx4e.ui.highlight.textOccurrence"; //$NON-NLS-1$
        }
    }

    private static String getAnnotationText(DocumentHighlightKind kind)
    {
        switch (kind)
        {
        case Write:
            return Messages.Highlighter_Write_occurrence;
        default:
            return Messages.Highlighter_Occurrence;
        }
    }

    private static ISnapshot getSnapshot(IDocument document)
    {
        if (!(document instanceof IDocumentExtension4))
            return null;
        return new DocumentSnapshot(document);
    }

    private ISelectionValidator getSelectionValidator()
    {
        return selectionProvider instanceof ISelectionValidator
            ? (ISelectionValidator)selectionProvider : null;
    }

    private class HighlightingJob
        extends Job
    {
        final LanguageOperationTarget target;
        final ISelection selection;
        final org.eclipse.lsp4j.Position position;

        HighlightingJob(LanguageOperationTarget target, ISelection selection,
            org.eclipse.lsp4j.Position position)
        {
            super("Highlighting Job"); //$NON-NLS-1$
            this.target = Objects.requireNonNull(target);
            this.selection = Objects.requireNonNull(selection);
            this.position = Objects.requireNonNull(position);
            setPriority(DECORATE);
            setSystem(true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            synchronized (jobLock)
            {
                if (monitor.isCanceled() || !isValid())
                    return Status.CANCEL_STATUS;
                List<? extends DocumentHighlight> highlights = computeHighlights(monitor);
                PlatformUI.getWorkbench().getDisplay().asyncExec(() ->
                {
                    if (!isValid())
                        return;

                    ISnapshot currentSnapshot = getSnapshot(viewer.getDocument());

                    if ((highlights == null || highlights.isEmpty()) && isSticky()
                        && snapshot != null && snapshot.isEqualTo(currentSnapshot))
                        return;

                    snapshot = currentSnapshot;
                    updateAnnotations(highlights);
                });
                return Status.OK_STATUS;
            }
        }

        private boolean isValid()
        {
            ISelectionValidator validator = getSelectionValidator();
            return validator == null || validator.isValid(selection)
                || selection == forcedSelection;
        }

        private List<? extends DocumentHighlight> computeHighlights(IProgressMonitor monitor)
        {
            URI documentUri = target.getDocumentUri();
            LanguageService languageService = target.getLanguageService();
            DocumentHighlightProvider provider = languageService.getDocumentMatcher().getBestMatch(
                languageService.getDocumentHighlightProviders(),
                DocumentHighlightProvider::getDocumentSelector, documentUri,
                target.getLanguageId());
            if (provider == null)
                return null;
            DocumentHighlightRequest request = newDocumentHighlightRequest();
            request.setProvider(provider);
            request.setParams(new DocumentHighlightParams(
                DocumentUri.toTextDocumentIdentifier(documentUri), position));
            request.setProgressMonitor(monitor);
            request.setMayThrow(false);
            return request.sendAndReceive();
        }
    }
}
