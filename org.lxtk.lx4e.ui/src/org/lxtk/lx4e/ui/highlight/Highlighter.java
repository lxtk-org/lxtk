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
package org.lxtk.lx4e.ui.highlight;

import static org.lxtk.lx4e.internal.util.AnnotationUtil.replaceAnnotations;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.handly.snapshot.DocumentSnapshot;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
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
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.ILinkedEditingListener;
import org.lxtk.lx4e.internal.ui.LinkedEditingPubSub;
import org.lxtk.lx4e.requests.DocumentHighlightRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;
import org.lxtk.util.Disposable;

/**
 * Highlights document ranges computed using {@link DocumentHighlightProvider}s.
 */
public class Highlighter
    implements Disposable
{
    private final ISourceViewer viewer;
    private final ISelectionProvider selectionProvider;
    private final Supplier<LanguageOperationTarget> targetSupplier;
    private final ILinkedEditingListener linkedEditingListener = new LinkedEditingListener();
    private final ISelectionChangedListener selectionChangedListener =
        e -> scheduleHighlighting(e.getSelection());
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
        LinkedEditingPubSub.INSTANCE.addLinkedEditingListener(linkedEditingListener);
    }

    @Override
    public void dispose()
    {
        LinkedEditingPubSub.INSTANCE.removeLinkedEditingListener(linkedEditingListener);
    }

    /**
     * Installs this highlighter by registering the necessary listeners
     * and scheduling a highlighting job for the current selection.
     */
    public final void install()
    {
        if (selectionProvider instanceof IPostSelectionProvider)
            ((IPostSelectionProvider)selectionProvider).addPostSelectionChangedListener(
                selectionChangedListener);
        else
            selectionProvider.addSelectionChangedListener(selectionChangedListener);

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
                selectionChangedListener);
        else
            selectionProvider.removeSelectionChangedListener(selectionChangedListener);

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
     * Returns the document highlight providers that match the given target.
     *
     * @param target never <code>null</code>
     * @return the matching document highlight providers (not <code>null</code>)
     */
    protected DocumentHighlightProvider[] getDocumentHighlightProviders(
        LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getDocumentHighlightProviders(),
            DocumentHighlightProvider::getDocumentSelector, target.getDocumentUri(),
            target.getLanguageId()).toArray(DocumentHighlightProvider[]::new);
    }

    /**
     * Computes the document highlight results for the given {@link DocumentHighlightParams}
     * using the given document highlight providers.
     *
     * @param providers never <code>null</code>
     * @param params never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed document highlight results, or <code>null</code> if none
     */
    protected DocumentHighlightResults computeDocumentHighlightResults(
        DocumentHighlightProvider[] providers, DocumentHighlightParams params,
        IProgressMonitor monitor)
    {
        if (providers.length == 0)
            return null;

        SubMonitor subMonitor = SubMonitor.convert(monitor, providers.length);

        Map<DocumentHighlightProvider, DocumentHighlightResult> results = new LinkedHashMap<>();

        for (DocumentHighlightProvider provider : providers)
        {
            DocumentHighlightResult result =
                computeDocumentHighlightResult(provider, params, subMonitor.split(1));

            results.put(provider, result);

            if (result != null)
            {
                List<? extends DocumentHighlight> highlights = result.getDocumentHighlights();
                if (highlights != null && !highlights.isEmpty())
                    break;
            }
        }

        return new DocumentHighlightResults(results);
    }

    /**
     * Computes a document highlight result for the given {@link DocumentHighlightParams}
     * using the given document highlight provider.
     *
     * @param provider never <code>null</code>
     * @param params never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed document highlight result, or <code>null</code> if none
     */
    protected DocumentHighlightResult computeDocumentHighlightResult(
        DocumentHighlightProvider provider, DocumentHighlightParams params,
        IProgressMonitor monitor)
    {
        DocumentHighlightRequest request = newDocumentHighlightRequest();
        request.setProvider(provider);
        // note that request params can get modified as part of request processing
        // (e.g. a progress token can be set); therefore we need to copy the given params
        request.setParams(JsonUtil.deepCopy(params));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
        request.setMayThrow(false);
        return new DocumentHighlightResult(request.sendAndReceive());
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

    final ITextViewer getViewer()
    {
        return viewer;
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

    /**
     * Represents a group of {@link DocumentHighlightResult}s.
     */
    protected static class DocumentHighlightResults
    {
        private final Map<DocumentHighlightProvider, DocumentHighlightResult> results;

        /**
         * Constructor.
         *
         * @param results not <code>null</code>
         */
        public DocumentHighlightResults(
            Map<DocumentHighlightProvider, DocumentHighlightResult> results)
        {
            this.results = Objects.requireNonNull(results);
        }

        /**
         * Returns the document highlight results as a map.
         *
         * @return the document highlight results as a map (never <code>null</code>)
         */
        public Map<DocumentHighlightProvider, DocumentHighlightResult> asMap()
        {
            return results;
        }
    }

    /**
     * Represents the result of a document highlight request.
     */
    protected static class DocumentHighlightResult
    {
        private final List<? extends DocumentHighlight> documentHighlights;

        /**
         * Constructor.
         *
         * @param documentHighlights may be <code>null</code>
         */
        public DocumentHighlightResult(List<? extends DocumentHighlight> documentHighlights)
        {
            this.documentHighlights = documentHighlights;
        }

        /**
         * Returns the document highlights for this result.
         *
         * @return the document highlights, or <code>null</code> if none
         */
        public List<? extends DocumentHighlight> getDocumentHighlights()
        {
            return documentHighlights;
        }
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
            DocumentHighlightResults results =
                computeDocumentHighlightResults(getDocumentHighlightProviders(target),
                    new DocumentHighlightParams(
                        DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()), position),
                    monitor);

            if (results == null)
                return null;

            for (Map.Entry<DocumentHighlightProvider,
                DocumentHighlightResult> entry : results.asMap().entrySet())
            {
                DocumentHighlightResult result = entry.getValue();
                if (result != null)
                {
                    List<? extends DocumentHighlight> highlights = result.getDocumentHighlights();
                    if (highlights != null && !highlights.isEmpty())
                        return highlights;
                }
            }

            return null;
        }
    }

    private class LinkedEditingListener
        implements ILinkedEditingListener
    {
        private boolean wasInstalled;

        @Override
        public void linkedEditingStarted(ITextViewer viewer)
        {
            if (viewer == getViewer() && (wasInstalled = isInstalled()))
                uninstall();
        }

        @Override
        public void linkedEditingStopped(ITextViewer viewer)
        {
            if (viewer == getViewer() && wasInstalled)
                install();
        }
    }
}
