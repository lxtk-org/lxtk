/*******************************************************************************
 * Copyright (c) 2020, 2021 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.folding;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.ui.PlatformUI;
import org.lxtk.DocumentUri;
import org.lxtk.FoldingRangeProvider;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.lx4e.requests.FoldingRangeRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;
import org.lxtk.util.Disposable;

/**
 * Manages annotations for document folding ranges computed using a {@link FoldingRangeProvider}.
 */
public class FoldingManager
{
    protected final ProjectionViewer viewer;
    protected final Supplier<LanguageOperationTarget> targetSupplier;
    private final IProjectionListener projectionListener = new IProjectionListener()
    {
        @Override
        public void projectionEnabled()
        {
            enable();
        }

        @Override
        public void projectionDisabled()
        {
            disable();
        }
    };
    private Disposable foldingSupport;

    /**
     * Constructor.
     *
     * @param viewer the target projection viewer for this manager
     *  (not <code>null</code>)
     * @param targetSupplier the {@link LanguageOperationTarget} supplier
     *  for this manager (not <code>null</code>)
     */
    public FoldingManager(ProjectionViewer viewer, Supplier<LanguageOperationTarget> targetSupplier)
    {
        this.viewer = Objects.requireNonNull(viewer);
        this.targetSupplier = Objects.requireNonNull(targetSupplier);
    }

    /**
     * Installs this manager by registering the necessary listeners
     * and scheduling a folding job.
     */
    public void install()
    {
        viewer.addProjectionListener(projectionListener);
        if (viewer.isProjectionMode())
            enable();
    }

    /**
     * Unistalls this manager by unregistering all listeners and removing folding.
     */
    public void uninstall()
    {
        viewer.removeProjectionListener(projectionListener);
        disable();
    }

    private void enable()
    {
        disable();
        foldingSupport = addFoldingSupport();
    }

    private void disable()
    {
        if (foldingSupport != null)
        {
            foldingSupport.dispose();
            foldingSupport = null;
        }
    }

    /**
     * Hook to add folding support.
     *
     * @return a disposable to remove folding support
     */
    protected Disposable addFoldingSupport()
    {
        IDocument document = viewer.getDocument();
        FoldingAnnotations annotations =
            newFoldingAnnotations(document, viewer.getProjectionAnnotationModel());
        Job job = new FoldingJob(annotations);
        IDocumentListener documentListener = new IDocumentListener()
        {
            @Override
            public void documentAboutToBeChanged(DocumentEvent event)
            {
            }

            @Override
            public void documentChanged(DocumentEvent event)
            {
                job.cancel();
                job.schedule(500);
            }
        };
        document.addDocumentListener(documentListener);
        job.schedule();

        return () ->
        {
            document.removeDocumentListener(documentListener);
            job.cancel();
            annotations.dispose();
        };
    }

    /**
     * Returns a new instance of {@link FoldingAnnotations}.
     *
     * @param document never <code>null</code>
     * @param model never <code>null</code>
     * @return the created object (not <code>null</code>)
     */
    protected FoldingAnnotations newFoldingAnnotations(IDocument document,
        ProjectionAnnotationModel model)
    {
        return new FoldingAnnotations(document, model);
    }

    /**
     * Returns a new instance of {@link FoldingRangeRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected FoldingRangeRequest newFoldingRangeRequest()
    {
        return new FoldingRangeRequest();
    }

    private class FoldingJob
        extends Job
    {
        private final FoldingAnnotations annotations;

        FoldingJob(FoldingAnnotations annotations)
        {
            super(FoldingJob.class.getName());
            this.annotations = Objects.requireNonNull(annotations);
            setPriority(DECORATE);
            setSystem(true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            List<FoldingRange> foldingRanges = computeFoldingRanges(monitor);
            PlatformUI.getWorkbench().getDisplay().asyncExec(
                () -> annotations.update(foldingRanges));
            return Status.OK_STATUS;
        }

        private List<FoldingRange> computeFoldingRanges(IProgressMonitor monitor)
        {
            LanguageOperationTarget target = targetSupplier.get();
            if (target == null)
                return null;

            URI documentUri = target.getDocumentUri();
            LanguageService languageService = target.getLanguageService();
            FoldingRangeProvider provider = languageService.getDocumentMatcher().getBestMatch(
                languageService.getFoldingRangeProviders(),
                FoldingRangeProvider::getDocumentSelector, documentUri, target.getLanguageId());
            if (provider == null)
                return null;

            FoldingRangeRequest request = newFoldingRangeRequest();
            request.setProvider(provider);
            request.setParams(
                new FoldingRangeRequestParams(DocumentUri.toTextDocumentIdentifier(documentUri)));
            request.setProgressMonitor(monitor);
            request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
            request.setMayThrow(false);
            return request.sendAndReceive();
        }
    }
}
