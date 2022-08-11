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
package org.lxtk.lx4e.refactoring.rename;

import java.util.Objects;
import java.util.UUID;
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
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.lxtk.DefaultWorkDoneProgress;
import org.lxtk.DocumentUri;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.LanguageService;
import org.lxtk.RenameProvider;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.lx4e.refactoring.WorkspaceEditRefactoring;
import org.lxtk.lx4e.requests.PrepareRenameRequest;
import org.lxtk.lx4e.requests.RenameRequest;

/**
 * Default implementation of a rename refactoring that uses {@link RenameProvider}s
 * to compute the rename edits.
 */
public class RenameRefactoring
    extends WorkspaceEditRefactoring
{
    private final LanguageOperationTarget target;
    private final IDocument document;
    private final int offset;
    private Position position;
    private RenameProvider[] renameProviders;
    private RenameProvider prepareRenameProvider;
    private Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> prepareRenameResult;
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
     * @param changeFactory the workspace edit change factory for the refactoring
     *  (not <code>null</code>)
     */
    public RenameRefactoring(String name, LanguageOperationTarget target, IDocument document,
        int offset, IWorkspaceEditChangeFactory changeFactory)
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
        if (currentName == null && prepareRenameResult != null
            && (prepareRenameResult.isFirst() || prepareRenameResult.isSecond()))
        {
            Range range = prepareRenameResult.getFirst();
            if (range == null)
                range = prepareRenameResult.getSecond().getRange();
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
        if (prepareRenameResult == null || !prepareRenameResult.isSecond())
            return getCurrentName();

        return prepareRenameResult.getSecond().getPlaceholder();
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
        return getRenameProviders().length != 0;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        setPrepareRenameResult(null, null);

        RenameProvider[] renameProviders = getRenameProviders();
        if (renameProviders.length == 0)
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_No_rename_provider);

        RefactoringStatus status = new RefactoringStatus();

        SubMonitor subMonitor = SubMonitor.convert(pm, renameProviders.length);

        for (int i = 0; i < renameProviders.length; i++)
        {
            RenameProvider renameProvider = renameProviders[i];

            if (!Boolean.TRUE.equals(renameProvider.getRegistrationOptions().getPrepareProvider()))
                return new RefactoringStatus();

            PrepareRenameRequest request = newPrepareRenameRequest();
            request.setProvider(renameProvider);
            PrepareRenameParams params = new PrepareRenameParams(
                DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()), getPosition());
            request.setParams(params);
            request.setProgressMonitor(subMonitor.split(1));

            Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> prepareRenameResult =
                null;
            try
            {
                prepareRenameResult = request.sendAndReceive();
            }
            catch (CompletionException e)
            {
                status.merge(handleError(e.getCause(), request.getErrorMessage()));
            }

            if (prepareRenameResult != null)
            {
                setPrepareRenameResult(renameProvider, prepareRenameResult);
                return new RefactoringStatus();
            }
        }

        // no prepare rename result
        if (!status.hasFatalError())
            status.addFatalError(Messages.RenameRefactoring_No_prepare_rename_result);
        return status;
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
        throws CoreException, OperationCanceledException
    {
        RenameProvider[] renameProviders = getRenameProviders();
        if (renameProviders.length == 0)
            return RefactoringStatus.createFatalErrorStatus(
                Messages.RenameRefactoring_No_rename_provider);

        RefactoringStatus status = checkNewName();
        if (status.hasFatalError())
            return status;

        WorkspaceEdit workspaceEdit = null;

        int i = 0;
        if (prepareRenameProvider != null)
            while (i < renameProviders.length && renameProviders[i] != prepareRenameProvider)
                i++;

        SubMonitor subMonitor = SubMonitor.convert(pm, renameProviders.length - i);

        while (i < renameProviders.length)
        {
            RenameProvider renameProvider = renameProviders[i++];

            RenameRequest request = newRenameRequest();
            request.setProvider(renameProvider);
            request.setParams(
                new RenameParams(DocumentUri.toTextDocumentIdentifier(target.getDocumentUri()),
                    getPosition(), getNewName()));
            request.setProgressMonitor(subMonitor.split(1));
            request.setUpWorkDoneProgress(
                () -> new DefaultWorkDoneProgress(Either.forLeft(UUID.randomUUID().toString())));

            try
            {
                workspaceEdit = request.sendAndReceive();
            }
            catch (CompletionException e)
            {
                status.merge(handleError(e.getCause(), request.getErrorMessage()));
            }

            if (workspaceEdit != null)
            {
                setWorkspaceEdit(workspaceEdit);

                return super.checkFinalConditions(null);
            }
        }

        // no workspace edit
        if (!status.hasFatalError())
            status.addFatalError(Messages.RenameRefactoring_No_workspace_edit);
        return status;
    }

    /**
     * Sets the result of preparing the rename operation.
     *
     * @param provider may be <code>null</code> iff the <code>result</code> is <code>null</code>
     * @param result may be <code>null</code> iff the <code>provider</code> is <code>null</code>
     */
    protected void setPrepareRenameResult(RenameProvider provider,
        Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior> result)
    {
        if (provider == null ^ result == null)
            throw new IllegalArgumentException();

        prepareRenameProvider = provider;
        prepareRenameResult = result;

        if (result != null)
            setCurrentName(null);
    }

    /**
     * Returns the rename provider that was used for preparing the rename operation.
     *
     * @return a {@link RenameProvider}, or <code>null</code> if none
     */
    protected final RenameProvider getPrepareRenameProvider()
    {
        return prepareRenameProvider;
    }

    /**
     * Returns the result of preparing the rename operation.
     *
     * @return the result of preparing the rename operation, or <code>null</code> if none
     */
    protected final Either3<Range, PrepareRenameResult,
        PrepareRenameDefaultBehavior> getPrepareRenameResult()
    {
        return prepareRenameResult;
    }

    /**
     * Returns a new instance of {@link PrepareRenameRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected PrepareRenameRequest newPrepareRenameRequest()
    {
        return new PrepareRenameRequest();
    }

    /**
     * Returns a new instance of {@link RenameRequest}.
     *
     * @return the created request object (not <code>null</code>)
     */
    protected RenameRequest newRenameRequest()
    {
        return new RenameRequest();
    }

    /**
     * Returns the rename providers for this refactoring.
     *
     * @return an array of rename providers (never <code>null</code>, never contains <code>null</code>s)
     */
    protected final RenameProvider[] getRenameProviders()
    {
        if (renameProviders == null)
            renameProviders = getRenameProviders(target);
        return renameProviders;
    }

    /**
     * Returns the rename providers for the given target.
     *
     * @param target never <code>null</code>
     * @return an array of rename providers (not <code>null</code>, does not contain <code>null</code>s)
     */
    protected RenameProvider[] getRenameProviders(LanguageOperationTarget target)
    {
        LanguageService languageService = target.getLanguageService();
        return languageService.getDocumentMatcher().getSortedMatches(
            languageService.getRenameProviders(), RenameProvider::getDocumentSelector,
            target.getDocumentUri(), target.getLanguageId()).toArray(RenameProvider[]::new);
    }

    /**
     * Returns the target document position for this refactoring.
     *
     * @return the target document position (never <code>null</code>)
     * @throws CoreException if there is no valid position
     */
    protected final Position getPosition() throws CoreException
    {
        if (position == null)
        {
            try
            {
                position = DocumentUtil.toPosition(document, offset);
            }
            catch (BadLocationException e)
            {
                throw new CoreException(Activator.createErrorStatus(e.getMessage(), e));
            }
        }
        return position;
    }

    /**
     * Handles the given exception by either returning a {@link RefactoringStatus} or throwing
     * a {@link CoreException}.
     *
     * @param e not <code>null</code>
     * @param msg may be <code>null</code>, in which case the message of the given exception is used
     * @return the corresponding refactoring status (never <code>null</code>)
     * @throws CoreException if returning a refactoring status is not appropriate for the given
     *  exception
     */
    protected static RefactoringStatus handleError(Throwable e, String msg) throws CoreException
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
