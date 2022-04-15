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
package org.lxtk.lx4e.ui.folding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
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
import org.lxtk.jsonrpc.JsonUtil;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.TaskExecutor;
import org.lxtk.lx4e.requests.FoldingRangeRequest;
import org.lxtk.lx4e.ui.WorkDoneProgressFactory;
import org.lxtk.util.Disposable;
import org.lxtk.util.Registry;
import org.lxtk.util.SafeRun;

/**
 * Manages annotations for document folding ranges computed using {@link FoldingRangeProvider}s.
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
        LanguageOperationTarget target = targetSupplier.get();
        IDocument document = viewer.getDocument();
        if (target == null || document == null)
            return null;

        return SafeRun.runWithResult(rollback ->
        {
            FoldingAnnotations annotations =
                newFoldingAnnotations(document, viewer.getProjectionAnnotationModel());
            Job job = new FoldingJob(target, annotations);

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
            rollback.add(() -> document.removeDocumentListener(documentListener));

            Consumer<FoldingRangeProvider> providersListener = provider ->
            {
                job.cancel();
                job.schedule();
            };
            Registry<FoldingRangeProvider> providers =
                target.getLanguageService().getFoldingRangeProviders();
            rollback.add(providers.onDidAdd().subscribe(providersListener)::dispose);
            rollback.add(providers.onDidRemove().subscribe(providersListener)::dispose);

            job.schedule();
            rollback.add(job::cancel);

            rollback.setLogger(e -> Activator.logError(e));
            return rollback::run;
        });
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
     * Returns folding range providers that match the given target.
     *
     * @param target never <code>null</code>
     * @return the matching folding range providers (not <code>null</code>)
     */
    protected FoldingRangeProvider[] getFoldingRangeProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getFoldingRangeProviders(), FoldingRangeProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(FoldingRangeProvider[]::new);
    }

    /**
     * Computes the folding range results for the given {@link FoldingRangeRequestParams}
     * using the given folding range providers.
     *
     * @param providers never <code>null</code>
     * @param params never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed folding range results, or <code>null</code> if none
     */
    protected FoldingRangeResults computeFoldingRangeResults(FoldingRangeProvider[] providers,
        FoldingRangeRequestParams params, IProgressMonitor monitor)
    {
        if (providers.length == 0)
            return null;

        return new FoldingRangeResults(TaskExecutor.parallelCompute(providers,
            (provider, taskMonitor) -> computeFoldingRangeResult(provider, params, taskMonitor),
            Messages.Computing_folding_ranges, null, monitor));
    }

    /**
     * Computes a folding range result for the given {@link FoldingRangeRequestParams}
     * using the given folding range provider.
     *
     * @param provider never <code>null</code>
     * @param params never <code>null</code>
     * @param monitor a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the computed folding range result, or <code>null</code> if none
     */
    protected FoldingRangeResult computeFoldingRangeResult(FoldingRangeProvider provider,
        FoldingRangeRequestParams params, IProgressMonitor monitor)
    {
        FoldingRangeRequest request = newFoldingRangeRequest();
        request.setProvider(provider);
        // note that request params can get modified as part of request processing
        // (e.g. a progress token can be set); therefore we need to copy the given params
        request.setParams(JsonUtil.deepCopy(params));
        request.setProgressMonitor(monitor);
        request.setUpWorkDoneProgress(WorkDoneProgressFactory::newWorkDoneProgress);
        request.setMayThrow(false);
        return new FoldingRangeResult(request.sendAndReceive());
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

    /**
     * Represents a group of {@link FoldingRangeResult}s.
     */
    protected static class FoldingRangeResults
    {
        private final Map<FoldingRangeProvider, FoldingRangeResult> results;

        /**
         * Constructor.
         *
         * @param results not <code>null</code>
         */
        public FoldingRangeResults(Map<FoldingRangeProvider, FoldingRangeResult> results)
        {
            this.results = Objects.requireNonNull(results);
        }

        /**
         * Returns the folding range results as a map.
         *
         * @return the folding range results as a map (never <code>null</code>)
         */
        public Map<FoldingRangeProvider, FoldingRangeResult> asMap()
        {
            return results;
        }
    }

    /**
     * Represents the result of a folding range request.
     */
    protected static class FoldingRangeResult
    {
        private final List<FoldingRange> foldingRanges;

        /**
         * Constructor.
         *
         * @param foldingRanges may be <code>null</code>
         */
        public FoldingRangeResult(List<FoldingRange> foldingRanges)
        {
            this.foldingRanges = foldingRanges;
        }

        /**
         * Returns a list of folding ranges.
         *
         * @return a list of folding ranges, or <code>null</code> if none
         */
        public List<FoldingRange> getFoldingRanges()
        {
            return foldingRanges;
        }
    }

    private class FoldingJob
        extends Job
    {
        private final LanguageOperationTarget target;
        private final FoldingAnnotations annotations;

        FoldingJob(LanguageOperationTarget target, FoldingAnnotations annotations)
        {
            super(FoldingJob.class.getName());
            this.target = Objects.requireNonNull(target);
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
            FoldingRangeResults results = computeFoldingRangeResults(
                getFoldingRangeProviders(target), new FoldingRangeRequestParams(
                    DocumentUri.toTextDocumentIdentifier(target.getDocumentUri())),
                monitor);
            if (results == null)
                return null;

            List<FoldingRange> allFoldingRanges = new ArrayList<>();
            results.asMap().forEach((provider, result) ->
            {
                if (result != null)
                {
                    List<FoldingRange> foldingRanges = result.getFoldingRanges();
                    if (foldingRanges != null)
                        allFoldingRanges.addAll(foldingRanges);
                }
            });
            return allFoldingRanges;
        }
    }
}
