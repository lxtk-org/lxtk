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
package org.lxtk.lx4e.ui.codeaction;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension2;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.jface.text.source.TextInvocationContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.lxtk.CommandService;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.DiagnosticMarkers;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.IDiagnosticAnnotation;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

import com.google.gson.Gson;

/**
 * TODO JavaDoc
 */
public abstract class AbstractQuickAssistProcessor
    implements IQuickAssistProcessor
{
    private String errorMessage;
    private Gson gson;

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(
        IQuickAssistInvocationContext invocationContext)
    {
        errorMessage = null;
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return null;
        ISourceViewer viewer = invocationContext.getSourceViewer();
        if (viewer == null)
            return null;
        IDocument document = viewer.getDocument();
        if (document == null)
            return null;
        Point selectedRange = viewer.getSelectedRange();
        int offset = invocationContext.getOffset();
        if (offset < 0)
            offset = selectedRange.x;
        int length = invocationContext.getLength();
        if (length < 0)
            length = selectedRange.y;
        Range range;
        try
        {
            range = DocumentUtil.toRange(document, offset, length);
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            return null;
        }
        CompletableFuture<List<Either<Command, CodeAction>>> future =
            CodeActions.getCodeActions(target, range, getCodeActionContext(
                new TextInvocationContext(viewer, offset, length)));
        List<Either<Command, CodeAction>> result = null;
        try
        {
            result = future.get(getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (CancellationException | InterruptedException e)
        {
        }
        catch (ExecutionException e)
        {
            Activator.logError(e);
            errorMessage = e.getMessage();
        }
        catch (TimeoutException e)
        {
            Activator.logWarning(e);
            errorMessage =
                Messages.AbstractQuickAssistProcessor_Request_timed_out;
        }
        if (result == null || result.isEmpty())
            return null;
        List<ICompletionProposal> proposals = new ArrayList<>(result.size());
        for (Either<Command, CodeAction> item : result)
        {
            if (item.isLeft())
                proposals.add(newProposal(item.getLeft()));
            else if (item.isRight())
                proposals.add(newProposal(item.getRight()));
        }
        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    @Override
    public boolean canFix(Annotation annotation)
    {
        if (annotation.isMarkedDeleted() || !isDiagnosticAnnotation(annotation))
            return false;
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return false;
        return CodeActions.hasCodeActionProvider(target);
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext)
    {
        LanguageOperationTarget target = getLanguageOperationTarget();
        if (target == null)
            return false;
        return CodeActions.hasCodeActionProvider(target);
    }

    @Override
    public String getErrorMessage()
    {
        return errorMessage;
    }

    /**
     * TODO JavaDoc
     *
     * @return the corresponding {@link LanguageOperationTarget},
     *  or <code>null</code> if none
     */
    protected abstract LanguageOperationTarget getLanguageOperationTarget();

    /**
     * TODO JavaDoc
     *
     * @return the command service (not <code>null</code>)
     */
    protected abstract CommandService getCommandService();

    /**
     * TODO JavaDoc
     *
     * @return a {@link WorkspaceEditChangeFactory} (not <code>null</code>)
     */
    protected abstract WorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

    /**
     * TODO JavaDoc
     *
     * @param invocationContext never <code>null</code>
     * @return the corresponding {@link CodeActionContext} (not <code>null</code>)
     */
    protected CodeActionContext getCodeActionContext(
        IQuickAssistInvocationContext invocationContext)
    {
        return new CodeActionContext(getDiagnostics(invocationContext));
    }

    /**
     * TODO JavaDoc
     *
     * @param invocationContext never <code>null</code>
     * @return the corresponding diagnostics (not <code>null</code>)
     */
    protected List<Diagnostic> getDiagnostics(
        IQuickAssistInvocationContext invocationContext)
    {
        ISourceViewer viewer = invocationContext.getSourceViewer();
        if (viewer == null)
            return Collections.emptyList();

        IAnnotationModel annotationModel =
            viewer instanceof ISourceViewerExtension2
                ? ((ISourceViewerExtension2)viewer).getVisualAnnotationModel()
                : viewer.getAnnotationModel();
        if (annotationModel == null)
            return Collections.emptyList();

        int offset = invocationContext.getOffset();
        int length = invocationContext.getLength();

        List<Diagnostic> result = new ArrayList<>();
        Iterator<Annotation> it =
            annotationModel instanceof IAnnotationModelExtension2
                ? ((IAnnotationModelExtension2)annotationModel).getAnnotationIterator(
                    offset, length, true, true)
                : annotationModel.getAnnotationIterator();
        while (it.hasNext())
        {
            Annotation annotation = it.next();
            Position position = annotationModel.getPosition(annotation);
            if (position != null && position.overlapsWith(offset, length)
                && isDiagnosticAnnotation(annotation))
            {
                Diagnostic diagnostic = getDiagnostic(annotation);
                if (diagnostic != null)
                    result.add(diagnostic);
            }
        }
        return result;
    }

    /**
     * TODO JavaDoc
     *
     * @param annotation never <code>null</code>
     * @return whether the given annotation is a diagnostic annotation
     */
    protected boolean isDiagnosticAnnotation(Annotation annotation)
    {
        if (annotation instanceof IDiagnosticAnnotation)
            return true;

        if (annotation instanceof SimpleMarkerAnnotation)
            return ((SimpleMarkerAnnotation)annotation).getMarker().getAttribute(
                DiagnosticMarkers.DIAGNOSTIC_ATTRIBUTE, null) != null;

        return false;
    }

    /**
     * TODO JavaDoc
     *
     * @param annotation never <code>null</code>
     * @return the corresponding diagnostic or <code>null</code> if none
     */
    protected Diagnostic getDiagnostic(Annotation annotation)
    {
        if (annotation instanceof IDiagnosticAnnotation)
            return ((IDiagnosticAnnotation)annotation).getDiagnostic();

        if (annotation instanceof SimpleMarkerAnnotation)
            return getDiagnostic(
                ((SimpleMarkerAnnotation)annotation).getMarker(),
                DiagnosticMarkers.DIAGNOSTIC_ATTRIBUTE);

        return null;
    }

    /**
     * TODO JavaDoc
     *
     * @param marker never <code>null</code>
     * @param diagnosticAttributeName never <code>null</code>
     * @return the diagnostic, or <code>null</code> if none
     */
    protected Diagnostic getDiagnostic(IMarker marker,
        String diagnosticAttributeName)
    {
        String value = marker.getAttribute(diagnosticAttributeName, null);
        if (value == null)
            return null;
        if (gson == null)
            gson = new Gson();
        return gson.fromJson(value, Diagnostic.class);
    }

    /**
     * TODO JavaDoc
     *
     * @param command never <code>null</code>
     * @return a completion proposal based on the command (not <code>null</code>)
     */
    protected ICompletionProposal newProposal(Command command)
    {
        return new CommandProposal(command);
    }

    /**
     * TODO JavaDoc
     *
     * @param codeAction never <code>null</code>
     * @return a completion proposal based on the code action (not <code>null</code>)
     */
    protected ICompletionProposal newProposal(CodeAction codeAction)
    {
        return new CodeActionProposal(codeAction);
    }

    /**
     * TODO JavaDoc
     *
     * @return a positive duration
     */
    protected Duration getTimeout()
    {
        return Duration.ofSeconds(2);
    }

    /**
     * TODO JavaDoc
     */
    protected class CommandProposal
        implements ICompletionProposal
    {
        protected final Command command;

        /**
         * TODO JavaDoc
         *
         * @param command not <code>null</code>
         */
        public CommandProposal(Command command)
        {
            this.command = Objects.requireNonNull(command);
        }

        @Override
        public void apply(IDocument document)
        {
            CodeActions.execute(command, getDisplayString(),
                getCommandService());
        }

        @Override
        public Point getSelection(IDocument document)
        {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo()
        {
            return null;
        }

        @Override
        public String getDisplayString()
        {
            return command.getTitle();
        }

        @Override
        public Image getImage()
        {
            return null;
        }

        @Override
        public IContextInformation getContextInformation()
        {
            return null;
        }
    }

    /**
     * TODO JavaDoc
     */
    protected class CodeActionProposal
        implements ICompletionProposal
    {
        protected final CodeAction codeAction;

        /**
         * TODO JavaDoc
         *
         * @param codeAction not <code>null</code>
         */
        public CodeActionProposal(CodeAction codeAction)
        {
            this.codeAction = Objects.requireNonNull(codeAction);
        }

        @Override
        public void apply(IDocument document)
        {
            CodeActions.execute(codeAction, getDisplayString(),
                getWorkspaceEditChangeFactory(), getCommandService());
        }

        @Override
        public Point getSelection(IDocument document)
        {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo()
        {
            return null;
        }

        @Override
        public String getDisplayString()
        {
            return codeAction.getTitle();
        }

        @Override
        public Image getImage()
        {
            return null;
        }

        @Override
        public IContextInformation getContextInformation()
        {
            return null;
        }
    }
}
