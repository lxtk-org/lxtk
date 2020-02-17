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
package org.lxtk.lx4e.refactoring.rename;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.RenameProvider;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.refactoring.WorkspaceEditRefactoring;
import org.lxtk.lx4e.util.EclipseFuture;

/**
 * TODO JavaDoc
 */
public class RenameRefactoring
    extends WorkspaceEditRefactoring
{
    private final LanguageOperationTarget target;
    private final IDocument document;
    private final int offset;
    private Position position;
    private RenameProvider renameProvider;
    private Either<Range, PrepareRenameResult> prepareRenameResult;
    private String newName, currentName;

    /**
     * TODO JavaDoc
     *
     * @param name the refactoring's name (not <code>null</code>)
     * @param target not <code>null</code>
     * @param document not <code>null</code>
     * @param offset 0-based
     * @param changeFactory not <code>null</code>
     */
    public RenameRefactoring(String name, LanguageOperationTarget target,
        IDocument document, int offset,
        WorkspaceEditChangeFactory changeFactory)
    {
        super(name, changeFactory);
        this.target = Objects.requireNonNull(target);
        this.document = Objects.requireNonNull(document);
        if (offset < 0)
            throw new IllegalArgumentException();
        this.offset = offset;
    }

    /**
     * TODO JavaDoc
     *
     * @return the refactoring's {@link LanguageOperationTarget}
     *  (never <code>null</code>)
     */
    public final LanguageOperationTarget getLanguageOperationTarget()
    {
        return target;
    }

    /**
     * TODO JavaDoc
     *
     * @return the target document for the refactoring (never <code>null</code>)
     */
    public final IDocument getDocument()
    {
        return document;
    }

    /**
     * TODO JavaDoc
     *
     * @return the target document offset for the refactoring (0-based)
     */
    public final int getOffset()
    {
        return offset;
    }

    /**
     * TODO JavaDoc
     *
     * @param newName the new name of the symbol to be renamed
     *  (not <code>null</code>)
     */
    public void setNewName(String newName)
    {
        this.newName = Objects.requireNonNull(newName);
    }

    /**
     * TODO JavaDoc
     *
     * @return the new name of the symbol to be renamed,
     *  or <code>null</code> if not set
     */
    public String getNewName()
    {
        return newName;
    }

    /**
     * TODO JavaDoc
     *
     * @return a refactoring status (never <code>null</code>)
     */
    public RefactoringStatus checkNewName()
    {
        String newName = getNewName();
        if (newName == null)
            throw new IllegalStateException("New name has not been set"); //$NON-NLS-1$

        if (newName.isEmpty())
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_New_name_is_empty);

        if (newName.equals(getCurrentName()))
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_New_name_is_equal_to_current_name);

        return new RefactoringStatus();
    }

    /**
     * TODO JavaDoc
     *
     * @param name the current name of the symbol to be renamed,
     *  or <code>null</code> if unknown
     */
    public void setCurrentName(String name)
    {
        currentName = name;
    }

    /**
     * TODO JavaDoc
     *
     * @return the current name of the symbol to be renamed,
     *  or <code>null</code> if unknown
     */
    public String getCurrentName()
    {
        if (currentName == null && prepareRenameResult != null)
        {
            Range range = prepareRenameResult.getLeft();
            if (range == null)
                range = prepareRenameResult.getRight().getRange();
            try
            {
                IRegion r = DocumentUtil.toRegion(document, range);
                setCurrentName(document.get(r.getOffset(), r.getLength()));
            }
            catch (BadLocationException e)
            {
                Activator.logError(e);
            }
        }
        return currentName;
    }

    /**
     * TODO JavaDoc
     *
     * @return the proposed new name of the symbol to be renamed,
     *  or <code>null</code> if none
     */
    public String getProposedNewName()
    {
        if (prepareRenameResult == null || !prepareRenameResult.isRight())
            return getCurrentName();

        return prepareRenameResult.getRight().getPlaceholder();
    }

    /**
     * TODO JavaDoc
     * <p>
     * This check should be fast.
     * </p>
     *
     * @return <code>true</code> if this refactoring is applicable to the target,
     *  and <code>false</code> otherwise
     */
    public boolean isApplicable()
    {
        return getRenameProvider() != null;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        prepareRenameResult = null;

        RenameProvider renameProvider = getRenameProvider();
        if (renameProvider == null)
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_No_rename_provider);

        if (!Boolean.TRUE.equals(
            renameProvider.getRegistrationOptions().getPrepareProvider()))
            return new RefactoringStatus();

        CompletableFuture<Either<Range, PrepareRenameResult>> future =
            renameProvider.prepareRename(new TextDocumentPositionParams(
                DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                getPosition()));
        try
        {
            prepareRenameResult = EclipseFuture.of(future).get(pm);
        }
        catch (InterruptedException e)
        {
            OperationCanceledException oce = new OperationCanceledException();
            oce.initCause(e);
            throw oce;
        }
        catch (ExecutionException e)
        {
            return handleError(e.getCause());
        }

        if (prepareRenameResult == null)
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_No_prepare_rename_result);

        setCurrentName(null);
        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        RenameProvider renameProvider = getRenameProvider();
        if (renameProvider == null)
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_No_rename_provider);

        RefactoringStatus status = checkNewName();
        if (status.hasFatalError())
            return status;

        CompletableFuture<WorkspaceEdit> future = renameProvider.getRenameEdits(
            new RenameParams(DocumentUri.toTextDocumentIdentifier(
                target.getDocumentUri()), getPosition(), getNewName()));

        SubMonitor monitor = SubMonitor.convert(pm, 100);

        WorkspaceEdit workspaceEdit;
        try
        {
            workspaceEdit = EclipseFuture.of(future).get(monitor.split(70));
        }
        catch (InterruptedException e)
        {
            OperationCanceledException oce = new OperationCanceledException();
            oce.initCause(e);
            throw oce;
        }
        catch (ExecutionException e)
        {
            return handleError(e.getCause());
        }

        if (workspaceEdit == null)
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_No_workspace_edit);

        setWorkspaceEdit(workspaceEdit);
        return super.checkFinalConditions(monitor.split(30));
    }

    private RenameProvider getRenameProvider()
    {
        if (renameProvider == null)
        {
            URI documentUri = target.getDocumentUri();
            LanguageService languageService = target.getLanguageService();
            renameProvider = languageService.getDocumentMatcher().getBestMatch(
                languageService.getRenameProviders(),
                RenameProvider::getDocumentSelector, documentUri,
                target.getLanguageId());
        }
        return renameProvider;
    }

    private Position getPosition() throws CoreException
    {
        if (position == null)
        {
            try
            {
                position = DocumentUtil.toPosition(document, offset);
            }
            catch (BadLocationException e)
            {
                throw new CoreException(Activator.createErrorStatus(
                    e.getMessage(), e));
            }
        }
        return position;
    }

    private static RefactoringStatus handleError(Throwable e)
        throws CoreException
    {
        if (e == null)
            return new RefactoringStatus();
        if (e instanceof ResponseErrorException)
        {
            Activator.logError(e);
            return RefactoringStatus.createFatalErrorStatus(e.getMessage());
        }
        throw new CoreException(Activator.createErrorStatus(e.getMessage(), e));
    }
}
