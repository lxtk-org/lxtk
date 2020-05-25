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
import java.util.concurrent.CompletionException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.lxtk.lx4e.requests.PrepareRenameRequest;
import org.lxtk.lx4e.requests.RenameRequest;

/**
 * Default implementation of a rename refactoring that uses a {@link
 * RenameProvider} to compute the rename edits.
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
     * Constructor.
     *
     * @param name the name of the refactoring (not <code>null</code>)
     * @param target the {@link LanguageOperationTarget} for the refactoring
     *  (not <code>null</code>)
     * @param document the target {@link IDocument} for the refactoring
     *  (not <code>null</code>)
     * @param offset the target document offset for the refactoring (0-based)
     * @param changeFactory the {@link WorkspaceEditChangeFactory}
     *  for the refactoring (not <code>null</code>)
     */
    public RenameRefactoring(String name, LanguageOperationTarget target, IDocument document,
        int offset, WorkspaceEditChangeFactory changeFactory)
    {
        super(name, changeFactory);
        this.target = Objects.requireNonNull(target);
        this.document = Objects.requireNonNull(document);
        if (offset < 0)
            throw new IllegalArgumentException();
        this.offset = offset;
    }

    /**
     * Returns the {@link LanguageOperationTarget} for this refactoring.
     *
     * @return the <code>LanguageOperationTarget</code> for the refactoring
     *  (never <code>null</code>)
     */
    public final LanguageOperationTarget getLanguageOperationTarget()
    {
        return target;
    }

    /**
     * Returns the target {@link IDocument} for this refactoring.
     *
     * @return the target document for the refactoring (never <code>null</code>)
     */
    public final IDocument getDocument()
    {
        return document;
    }

    /**
     * Returns the target document offset for this refactoring.
     *
     * @return the target document offset for the refactoring (0-based)
     */
    public final int getOffset()
    {
        return offset;
    }

    /**
     * Sets the new name of the document symbol to be renamed.
     *
     * @param newName the new name of the document symbol to be renamed
     *  (not <code>null</code>)
     */
    public void setNewName(String newName)
    {
        this.newName = Objects.requireNonNull(newName);
    }

    /**
     * Returns the new name of the document symbol to be renamed.
     *
     * @return the new name of the document symbol to be renamed,
     *  or <code>null</code> if it has not been set
     * @see #setNewName(String)
     */
    public String getNewName()
    {
        return newName;
    }

    /**
     * Checks whether the {@link #getNewName() new name} is valid
     * for the document symbol to be renamed.
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
     * Sets the current name of the document symbol to be renamed.
     *
     * @param name the current name of the document symbol to be renamed,
     *  or <code>null</code> if unknown
     */
    public void setCurrentName(String name)
    {
        currentName = name;
    }

    /**
     * Returns the current name of the document symbol to be renamed.
     *
     * @return the current name of the document symbol to be renamed,
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
     * Returns the proposed new name of the document symbol to be renamed.
     *
     * @return the proposed new name of the document symbol to be renamed,
     *  or <code>null</code> if none
     */
    public String getProposedNewName()
    {
        if (prepareRenameResult == null || !prepareRenameResult.isRight())
            return getCurrentName();

        return prepareRenameResult.getRight().getPlaceholder();
    }

    /**
     * Checks whether this refactoring is applicable.
     * <p>
     * Implementation note: This check should be fast.
     * </p>
     *
     * @return <code>true</code> if the refactoring is applicable,
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

        if (!Boolean.TRUE.equals(renameProvider.getRegistrationOptions().getPrepareProvider()))
            return new RefactoringStatus();

        PrepareRenameRequest request = newPrepareRenameRequest();
        request.setProvider(renameProvider);
        request.setParams(new TextDocumentPositionParams(
            DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()), getPosition()));
        request.setProgressMonitor(pm);

        try
        {
            prepareRenameResult = request.sendAndReceive();
        }
        catch (CompletionException e)
        {
            return handleError(e.getCause(), request.getErrorMessage());
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

        SubMonitor monitor = SubMonitor.convert(pm, 100);

        RenameRequest request = newRenameRequest();
        request.setProvider(renameProvider);
        request.setParams(
            new RenameParams(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                getPosition(), getNewName()));
        request.setProgressMonitor(monitor.split(70));

        WorkspaceEdit workspaceEdit;
        try
        {
            workspaceEdit = request.sendAndReceive();
        }
        catch (CompletionException e)
        {
            return handleError(e.getCause(), request.getErrorMessage());
        }

        if (workspaceEdit == null)
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_No_workspace_edit);

        setWorkspaceEdit(workspaceEdit);
        return super.checkFinalConditions(monitor.split(30));
    }

    /**
     * Returns a request for preparing rename operation.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected PrepareRenameRequest newPrepareRenameRequest()
    {
        return new PrepareRenameRequest();
    }

    /**
     * Returns a request for computing rename edits.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected RenameRequest newRenameRequest()
    {
        return new RenameRequest();
    }

    private RenameProvider getRenameProvider()
    {
        if (renameProvider == null)
        {
            URI documentUri = target.getDocumentUri();
            LanguageService languageService = target.getLanguageService();
            renameProvider = languageService.getDocumentMatcher().getBestMatch(
                languageService.getRenameProviders(), RenameProvider::getDocumentSelector,
                documentUri, target.getLanguageId());
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
                throw Activator.toCoreException(e, e.getMessage());
            }
        }
        return position;
    }

    private static RefactoringStatus handleError(Throwable e, String msg) throws CoreException
    {
        if (msg == null)
            msg = e.getMessage();

        IStatus status = Activator.createErrorStatus(msg, e);

        if (e instanceof ResponseErrorException)
        {
            Activator.getDefault().getLog().log(status);
            return RefactoringStatus.createFatalErrorStatus(e.getMessage());
        }

        throw new CoreException(status);
    }
}
