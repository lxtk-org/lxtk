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
 *     Alexander Kozinko (1C)
 *******************************************************************************/
package org.lxtk.lx4e.ui.codeaction;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.lxtk.CodeActionProvider;
import org.lxtk.CommandService;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.jsonrpc.DefaultGson;
import org.lxtk.lx4e.CodeActionRequest;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.diagnostics.DiagnosticMarkers;
import org.lxtk.lx4e.diagnostics.IDiagnosticAnnotation;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;

/**
 * Partial implementation of an {@link IQuickAssistProcessor} that computes
 * quick fixes and quick assists using a {@link CodeActionProvider}.
 */
public abstract class AbstractQuickAssistProcessor
    implements IQuickAssistProcessor
{
    private String errorMessage;

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

        CodeActionProvider provider = CodeActions.getCodeActionProvider(target);
        if (provider == null)
            return null;

        CodeActionRequest request = newCodeActionRequest();
        request.setProvider(provider);
        request.setParams(
            new CodeActionParams(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                range, getCodeActionContext(new TextInvocationContext(viewer, offset, length))));
        request.setTimeout(getCodeActionTimeout());
        request.setMayThrow(false);

        List<Either<Command, CodeAction>> result = request.sendAndReceive();
        errorMessage = request.getErrorMessage();

        if (result == null || result.isEmpty())
            return null;

        List<ICompletionProposal> proposals = new ArrayList<>(result.size());
        for (Either<Command, CodeAction> item : result)
        {
            if (item.isLeft())
                proposals.add(newProposal(item.getLeft(), provider));
            else if (item.isRight())
                proposals.add(newProposal(item.getRight(), provider));
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
     * Returns the current {@link LanguageOperationTarget}.
     *
     * @return the current <code>LanguageOperationTarget</code>,
     *  or <code>null</code> if none
     */
    protected abstract LanguageOperationTarget getLanguageOperationTarget();

    /**
     * Returns the {@link WorkspaceEditChangeFactory} for this processor.
     *
     * @return the <code>WorkspaceEditChangeFactory</code> (not <code>null</code>)
     */
    protected abstract WorkspaceEditChangeFactory getWorkspaceEditChangeFactory();

    /**
     * Returns a new instance of {@link CodeActionRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected CodeActionRequest newCodeActionRequest()
    {
        return new CodeActionRequest();
    }

    /**
     * Returns the timeout for a code action request.
     *
     * @return a positive duration
     */
    protected Duration getCodeActionTimeout()
    {
        return Duration.ofSeconds(2);
    }

    /**
     * Returns the {@link CodeActionContext} corresponding to the given
     * {@link IQuickAssistInvocationContext}.
     *
     * @param invocationContext never <code>null</code>
     * @return the corresponding <code>CodeActionContext</code> (not <code>null</code>)
     */
    protected CodeActionContext getCodeActionContext(
        IQuickAssistInvocationContext invocationContext)
    {
        return new CodeActionContext(getDiagnostics(invocationContext));
    }

    /**
     * Given an {@link IQuickAssistInvocationContext}, returns a list
     * of the corresponding {@link Diagnostic}s.
     *
     * @param invocationContext never <code>null</code>
     * @return the corresponding diagnostics (not <code>null</code>)
     */
    protected List<Diagnostic> getDiagnostics(IQuickAssistInvocationContext invocationContext)
    {
        ISourceViewer viewer = invocationContext.getSourceViewer();
        if (viewer == null)
            return Collections.emptyList();

        IAnnotationModel annotationModel = viewer instanceof ISourceViewerExtension2
            ? ((ISourceViewerExtension2)viewer).getVisualAnnotationModel()
            : viewer.getAnnotationModel();
        if (annotationModel == null)
            return Collections.emptyList();

        int offset = invocationContext.getOffset();
        int length = invocationContext.getLength();

        List<Diagnostic> result = new ArrayList<>();
        Iterator<Annotation> it = annotationModel instanceof IAnnotationModelExtension2
            ? ((IAnnotationModelExtension2)annotationModel).getAnnotationIterator(offset, length,
                true, true)
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
     * Checks whether the given annotation represents a diagnostic.
     *
     * @param annotation never <code>null</code>
     * @return <code>true</code> if the given annotation is a diagnostic
     *  annotation, and <code>false</code> otherwise
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
     * Returns the diagnostic represented by the given annotation.
     *
     * @param annotation never <code>null</code>
     * @return the requested diagnostic, or <code>null</code> if none
     */
    protected Diagnostic getDiagnostic(Annotation annotation)
    {
        if (annotation instanceof IDiagnosticAnnotation)
            return ((IDiagnosticAnnotation)annotation).getDiagnostic();

        if (annotation instanceof SimpleMarkerAnnotation)
            return getDiagnostic(((SimpleMarkerAnnotation)annotation).getMarker(),
                DiagnosticMarkers.DIAGNOSTIC_ATTRIBUTE);

        return null;
    }

    /**
     * Returns the diagnostic contained in the given marker attribute.
     *
     * @param marker never <code>null</code>
     * @param diagnosticAttributeName never <code>null</code>
     * @return the requested diagnostic, or <code>null</code> if none
     */
    protected Diagnostic getDiagnostic(IMarker marker, String diagnosticAttributeName)
    {
        String value = marker.getAttribute(diagnosticAttributeName, null);
        if (value == null)
            return null;
        return DefaultGson.INSTANCE.fromJson(value, Diagnostic.class);
    }

    /**
     * Creates and returns a proposal that executes the given {@link Command}.
     *
     * @param command never <code>null</code>
     * @param provider never <code>null</code>
     * @return the created proposal (not <code>null</code>)
     */
    protected ICompletionProposal newProposal(Command command, CodeActionProvider provider)
    {
        return new CommandProposal(command, provider);
    }

    /**
     * Creates and returns a proposal that executes the given {@link CodeAction}.
     *
     * @param codeAction never <code>null</code>
     * @param provider never <code>null</code>
     * @return the created proposal (not <code>null</code>)
     */
    protected ICompletionProposal newProposal(CodeAction codeAction, CodeActionProvider provider)
    {
        return new CodeActionProposal(codeAction, provider);
    }

    /**
     * A proposal that executes a given {@link Command}.
     */
    protected class CommandProposal
        implements ICompletionProposal
    {
        /**
         * The associated {@link Command} object (never <code>null</code>).
         */
        protected final Command command;

        private final CommandService commandService;

        /**
         * Constructor.
         *
         * @param command not <code>null</code>
         * @param provider not <code>null</code>
         */
        public CommandProposal(Command command, CodeActionProvider provider)
        {
            this.command = Objects.requireNonNull(command);
            this.commandService = provider.getCommandService();
        }

        @Override
        public void apply(IDocument document)
        {
            CodeActions.execute(command, getDisplayString(), commandService);
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
     * A proposal that executes a given {@link CodeAction}.
     */
    protected class CodeActionProposal
        implements ICompletionProposal
    {
        /**
         * The associated {@link CodeAction} object (never <code>null</code>).
         */
        protected final CodeAction codeAction;

        private final CommandService commandService;

        /**
         * Constructor.
         *
         * @param codeAction not <code>null</code>
         * @param provider not <code>null</code>
         */
        public CodeActionProposal(CodeAction codeAction, CodeActionProvider provider)
        {
            this.codeAction = Objects.requireNonNull(codeAction);
            this.commandService = provider.getCommandService();
        }

        @Override
        public void apply(IDocument document)
        {
            CodeActions.execute(codeAction, getDisplayString(), getWorkspaceEditChangeFactory(),
                commandService);
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
