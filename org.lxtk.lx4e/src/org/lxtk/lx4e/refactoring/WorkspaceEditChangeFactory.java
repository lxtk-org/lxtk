/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
package org.lxtk.lx4e.refactoring;

import static org.lxtk.lx4e.UriHandlers.compose;
import static org.lxtk.lx4e.UriHandlers.getBuffer;
import static org.lxtk.lx4e.UriHandlers.toDisplayString;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.snapshot.DocumentSnapshot;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4j.AnnotatedTextEdit;
import org.eclipse.lsp4j.ChangeAnnotation;
import org.eclipse.lsp4j.CreateFile;
import org.eclipse.lsp4j.DeleteFile;
import org.eclipse.lsp4j.RenameFile;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.lxtk.DocumentService;
import org.lxtk.DocumentUri;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentSnapshot;
import org.lxtk.WorkspaceEditUtil;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.EfsUriHandler;
import org.lxtk.lx4e.IUriHandler;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.ResourceUriHandler;
import org.lxtk.lx4e.TextDocumentUriHandler;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.lx4e.internal.util.ChangeUtil;

/**
 * Default implementation of {@link IWorkspaceEditChangeFactory}.
 */
public class WorkspaceEditChangeFactory
    implements IWorkspaceEditChangeFactory
{
    /**
     * The associated {@link DocumentService} (never <code>null</code>).
     */
    protected final DocumentService documentService;
    /**
     * The associated {@link IUriHandler} (never <code>null</code>).
     */
    protected final IUriHandler uriHandler;

    /**
     * Creates a new factory instance with the given {@link DocumentService} and a
     * default {@link IUriHandler}.
     *
     * @param documentService not <code>null</code>
     */
    public WorkspaceEditChangeFactory(DocumentService documentService)
    {
        this(documentService, compose(new TextDocumentUriHandler(documentService),
            new ResourceUriHandler(), new EfsUriHandler()));
    }

    /**
     * Creates a new factory instance with the given {@link DocumentService}
     * and the given {@link IUriHandler}.
     *
     * @param documentService not <code>null</code>
     * @param uriHandler not <code>null</code>
     */
    public WorkspaceEditChangeFactory(DocumentService documentService, IUriHandler uriHandler)
    {
        this.documentService = Objects.requireNonNull(documentService);
        this.uriHandler = Objects.requireNonNull(uriHandler);
    }

    @Override
    public Change createChange(String name, WorkspaceEdit workspaceEdit, IProgressMonitor pm)
        throws CoreException
    {
        return getChangeCreator(workspaceEdit).createChange(name, pm);
    }

    /**
     * Returns a {@link ChangeCreator} for the given {@link WorkspaceEdit}.
     *
     * @param workspaceEdit never <code>null</code>
     * @return the change creator (not <code>null</code>)
     */
    protected ChangeCreator getChangeCreator(WorkspaceEdit workspaceEdit)
    {
        return new ChangeCreator(workspaceEdit);
    }

    // undo changes are assumed to have been initialized
    private static Boolean rollback(List<Change> undos)
    {
        if (undos == null)
            return Boolean.FALSE; // cannot rollback
        if (undos.isEmpty())
            return null; // nothing to rollback
        Collections.reverse(undos);
        for (Change undo : undos)
        {
            try
            {
                ChangeUtil.safelyDisposeChange(ChangeUtil.executeChange(undo, false, false, null));
            }
            catch (OperationCanceledException e) // should not happen
            {
                return Boolean.FALSE;
            }
            catch (Exception | LinkageError | AssertionError e) // same as in SafeRunner
            {
                Activator.logError(e);
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    private static CoreException newChangeExecutionFailedException(String changeName,
        Throwable cause, Boolean rollbackResult)
    {
        return new CoreException(Activator.createErrorStatus(MessageFormat.format(
            rollbackResult == null ? Messages.WorkspaceEditChangeFactory_Change_execution_failed
                : (Boolean.TRUE.equals(rollbackResult)
                    ? Messages.WorkspaceEditChangeFactory_Change_execution_failed_and_has_been_rolled_back
                    : Messages.WorkspaceEditChangeFactory_Change_execution_failed_and_could_not_be_rolled_back),
            changeName), cause));
    }

    /**
     * Actually creates a {@link Change} object for a given {@link WorkspaceEdit}.
     */
    protected class ChangeCreator
    {
        /**
         * The {@link WorkspaceEdit} to process.
         */
        protected final WorkspaceEdit workspaceEdit;
        private Map<String, GroupCategorySet> groupCategories;

        /**
         * Constructor.
         *
         * @param workspaceEdit not <code>null</code>
         */
        public ChangeCreator(WorkspaceEdit workspaceEdit)
        {
            this.workspaceEdit = Objects.requireNonNull(workspaceEdit);
        }

        /**
         * Creates a {@link Change} object that performs the workspace transformation
         * described by the workspace edit being processed.
         *
         * @param name the human readable name of the change. Will
         *  be used to display the change in the user interface
         * @param pm a progress monitor, or <code>null</code>
         *  if progress reporting is not desired. The caller must not rely on
         *  {@link IProgressMonitor#done()} having been called by the receiver
         * @return the created change (never <code>null</code>)
         * @throws CoreException if this method could not create a change
         * @throws OperationCanceledException if this method is canceled
         */
        public Change createChange(String name, IProgressMonitor pm) throws CoreException
        {
            if (WorkspaceEditUtil.hasResourceOperations(workspaceEdit))
                return new WorkspaceEditChange(name);

            CompositeChange change = new CompositeChangeWithRollback(name);
            change.markAsSynthetic();

            List<Either<TextDocumentEdit, ResourceOperation>> documentChanges =
                workspaceEdit.getDocumentChanges();
            if (documentChanges != null)
            {
                SubMonitor monitor = SubMonitor.convert(pm, documentChanges.size());
                for (Either<TextDocumentEdit, ResourceOperation> documentChange : documentChanges)
                {
                    TextDocumentEdit textDocumentEdit = documentChange.getLeft();
                    if (textDocumentEdit == null)
                        throw new AssertionError(); // should never happen
                    change.add(createChange(textDocumentEdit));
                    monitor.split(1);
                }
            }
            else
            {
                Map<String, List<TextEdit>> changes = workspaceEdit.getChanges();
                if (changes != null)
                {
                    SubMonitor monitor = SubMonitor.convert(pm, changes.size());
                    for (Entry<String, List<TextEdit>> entry : changes.entrySet())
                    {
                        change.add(createChange(new TextDocumentEdit(
                            new VersionedTextDocumentIdentifier(entry.getKey(), null),
                            entry.getValue())));
                        monitor.split(1);
                    }
                }
            }
            return change;
        }

        /**
         * Creates a {@link Change} corresponding to the given {@link TextDocumentEdit}.
         *
         * @param textDocumentEdit a <code>TextDocumentEdit</code> (never <code>null</code>)
         * @return the created change (not <code>null</code>)
         * @throws CoreException if this method could not create a change
         */
        protected Change createChange(TextDocumentEdit textDocumentEdit) throws CoreException
        {
            VersionedTextDocumentIdentifier textDocumentId = textDocumentEdit.getTextDocument();
            URI uri = DocumentUri.convert(textDocumentId.getUri());

            IDocument document;
            ISnapshot snapshot;
            TextDocument textDocument = documentService.getTextDocument(uri);
            if (textDocument != null)
            {
                TextDocumentSnapshot textDocumentSnapshot =
                    textDocument.getLastChange().getSnapshot();
                if (textDocumentId.getVersion() != null
                    && textDocumentId.getVersion().intValue() != textDocumentSnapshot.getVersion())
                {
                    throw new CoreException(Activator.createErrorStatus(MessageFormat.format(
                        Messages.WorkspaceEditChangeFactory_Stale_workspace_edit,
                        toDisplayString(uri, uriHandler))));
                }
                document = new Document(textDocumentSnapshot.getText());
                snapshot = new DocumentSnapshot(document);
            }
            else
            {
                if (Boolean.FALSE.equals(uriHandler.exists(uri)))
                {
                    throw new CoreException(Activator.createErrorStatus(MessageFormat.format(
                        Messages.WorkspaceEditChangeFactory_File_does_not_exist,
                        toDisplayString(uri, uriHandler))));
                }

                try (IBuffer buffer = getBuffer(uri, uriHandler))
                {
                    NonExpiringSnapshot nonExpiringSnapshot = new NonExpiringSnapshot(buffer);
                    document = new Document(nonExpiringSnapshot.getContents());
                    snapshot = nonExpiringSnapshot.getWrappedSnapshot();
                }
            }

            TextFileChange change =
                new TextFileChange(toDisplayString(uri, uriHandler), uri, uriHandler);
            MultiTextEdit rootEdit = new MultiTextEdit();
            change.setEdit(rootEdit);
            change.setBase(snapshot);

            try
            {
                for (TextEdit edit : textDocumentEdit.getEdits())
                {
                    IRegion r = DocumentUtil.toRegion(document, edit.getRange());
                    ReplaceEdit childEdit =
                        new ReplaceEdit(r.getOffset(), r.getLength(), edit.getNewText());
                    rootEdit.addChild(childEdit);

                    GroupCategorySet groupCategorySet = edit instanceof AnnotatedTextEdit
                        ? getGroupCategorySet(((AnnotatedTextEdit)edit).getAnnotationId())
                        : GroupCategorySet.NONE;
                    TextEditChangeGroup group =
                        new TextEditChangeGroup(change, new CategorizedTextEditGroup(
                            getGroupName(edit), childEdit, groupCategorySet));
                    group.setEnabled(isGroupEnabled(edit));
                    change.addTextEditChangeGroup(group);
                }
            }
            catch (MalformedTreeException | BadLocationException e)
            {
                throw new CoreException(Activator.createErrorStatus(e.getMessage(), e));
            }

            return change;
        }

        /**
         * Creates a {@link Change} corresponding to the given {@link ResourceOperation}.
         *
         * @param operation a <code>ResourceOperation</code> (never <code>null</code>)
         * @return the created change (not <code>null</code>)
         * @throws CoreException if this method could not create a change
         */
        protected Change createChange(ResourceOperation operation) throws CoreException
        {
            if (operation instanceof CreateFile)
                return createChange((CreateFile)operation);

            if (operation instanceof DeleteFile)
                return createChange((DeleteFile)operation);

            if (operation instanceof RenameFile)
                return createChange((RenameFile)operation);

            throw new CoreException(Activator.createErrorStatus(MessageFormat.format(
                Messages.WorkspaceEditChangeFactory_Unsupported_resource_operation_kind,
                operation.getKind())));
        }

        /**
         * Creates a {@link Change} corresponding to the given {@link CreateFile} operation.
         *
         * @param createFile a <code>CreateFile</code> operation (never <code>null</code>)
         * @return the created change (not <code>null</code>)
         * @throws CoreException if this method could not create a change
         */
        protected Change createChange(CreateFile createFile) throws CoreException
        {
            URI uri = DocumentUri.convert(createFile.getUri());

            Change change = uriHandler.getCreateFileChange(uri, createFile.getOptions());
            if (change == null)
            {
                throw new CoreException(Activator.createErrorStatus((MessageFormat.format(
                    Messages.WorkspaceEditChangeFactory_Unsupported_create_operation,
                    toDisplayString(uri, uriHandler)))));
            }

            return change;
        }

        /**
         * Creates a {@link Change} corresponding to the given {@link DeleteFile} operation.
         *
         * @param deleteFile a <code>DeleteFile</code> operation (never <code>null</code>)
         * @return the created change (not <code>null</code>)
         * @throws CoreException if this method could not create a change
         */
        protected Change createChange(DeleteFile deleteFile) throws CoreException
        {
            URI uri = DocumentUri.convert(deleteFile.getUri());

            Change change = uriHandler.getDeleteFileChange(uri, deleteFile.getOptions());
            if (change == null)
            {
                throw new CoreException(Activator.createErrorStatus((MessageFormat.format(
                    Messages.WorkspaceEditChangeFactory_Unsupported_delete_operation,
                    toDisplayString(uri, uriHandler)))));
            }

            return change;
        }

        /**
         * Creates a {@link Change} corresponding to the given {@link RenameFile} operation.
         *
         * @param renameFile a <code>RenameFile</code> operation (never <code>null</code>)
         * @return the created change (not <code>null</code>)
         * @throws CoreException if this method could not create a change
         */
        protected Change createChange(RenameFile renameFile) throws CoreException
        {
            URI uri = DocumentUri.convert(renameFile.getOldUri());
            URI newUri = DocumentUri.convert(renameFile.getNewUri());

            Change change = uriHandler.getRenameFileChange(uri, newUri, renameFile.getOptions());
            if (change == null)
            {
                throw new CoreException(Activator.createErrorStatus((MessageFormat.format(
                    Messages.WorkspaceEditChangeFactory_Unsupported_rename_operation,
                    toDisplayString(uri, uriHandler), toDisplayString(newUri, uriHandler)))));
            }

            return change;
        }

        /**
         * Returns the group name for the given {@link TextEdit}.
         *
         * @param edit never <code>null</code>
         * @return the text edit group name (not <code>null</code>)
         */
        protected String getGroupName(TextEdit edit)
        {
            Boolean needsConfirmation = null;
            if (edit instanceof AnnotatedTextEdit)
            {
                ChangeAnnotation annotation = WorkspaceEditUtil.getChangeAnnotation(workspaceEdit,
                    ((AnnotatedTextEdit)edit).getAnnotationId());
                if (annotation != null)
                    needsConfirmation = annotation.getNeedsConfirmation();
            }
            if (Boolean.TRUE.equals(needsConfirmation))
                return Messages.WorkspaceEditChangeFactory_Text_edit_group_potential;
            else if (Boolean.FALSE.equals(needsConfirmation))
                return Messages.WorkspaceEditChangeFactory_Text_edit_group_exact;
            else
                return Messages.WorkspaceEditChangeFactory_Text_edit_group_default;
        }

        /**
         * Returns whether the group for the given {@link TextEdit} should be initially enabled.
         *
         * @param edit never <code>null</code>
         * @return <code>true</code> if the text edit group should be enabled,
         *  and <code>false</code> if it should be disabled
         */
        protected boolean isGroupEnabled(TextEdit edit)
        {
            return true;
        }

        /**
         * Given a change annotation identifier, returns the corresponding group category set,
         * {@link #createGroupCategorySet(String) creating} it if necessary.
         *
         * @param changeAnnotationId never <code>null</code>
         * @return the corresponding group category set (may be empty, not <code>null</code>)
         */
        protected final GroupCategorySet getGroupCategorySet(String changeAnnotationId)
        {
            if (groupCategories == null)
                groupCategories = new HashMap<>();
            return groupCategories.computeIfAbsent(changeAnnotationId,
                this::createGroupCategorySet);
        }

        /**
         * Creates and returns the group category set for the given change annotation identifier.
         *
         * @param changeAnnotationId never <code>null</code>
         * @return the created group category set (may be empty, not <code>null</code>)
         */
        protected GroupCategorySet createGroupCategorySet(String changeAnnotationId)
        {
            ChangeAnnotation changeAnnotation =
                WorkspaceEditUtil.getChangeAnnotation(workspaceEdit, changeAnnotationId);
            return changeAnnotation == null ? GroupCategorySet.NONE
                : new GroupCategorySet(
                    new GroupCategory(changeAnnotationId, changeAnnotation.getLabel(),
                        Optional.ofNullable(changeAnnotation.getDescription()).orElse(
                            changeAnnotation.getLabel())));
        }

        private Change createChange(Either<TextDocumentEdit, ResourceOperation> documentChange)
            throws CoreException
        {
            if (documentChange.isLeft())
                return createChange(documentChange.getLeft());

            return createChange(documentChange.getRight());
        }

        private class WorkspaceEditChange
            extends Change
        {
            private final String name;

            WorkspaceEditChange(String name)
            {
                this.name = Objects.requireNonNull(name);
            }

            @Override
            public String getName()
            {
                return name;
            }

            @Override
            public void initializeValidationData(IProgressMonitor pm)
            {
            }

            @Override
            public RefactoringStatus isValid(IProgressMonitor pm)
                throws CoreException, OperationCanceledException
            {
                return new RefactoringStatus();
            }

            @Override
            public Change perform(IProgressMonitor pm) throws CoreException
            {
                List<Either<TextDocumentEdit, ResourceOperation>> documentChanges =
                    workspaceEdit.getDocumentChanges();
                List<Change> undos = new ArrayList<>();
                try
                {
                    SubMonitor subMonitor = SubMonitor.convert(pm, documentChanges.size());
                    for (Either<TextDocumentEdit,
                        ResourceOperation> documentChange : documentChanges)
                    {
                        Change change = createChange(documentChange);
                        Change undo;
                        try
                        {
                            undo = ChangeUtil.executeChange(change, true, undos != null,
                                subMonitor.split(1));
                        }
                        catch (OperationCanceledException e)
                        {
                            rollback(undos);
                            throw e;
                        }
                        catch (Exception | LinkageError | AssertionError e) // same as in SafeRunner
                        {
                            Activator.logError(e);

                            Boolean rollbackResult = rollback(undos);

                            throw newChangeExecutionFailedException(name, e, rollbackResult);
                        }
                        finally
                        {
                            ChangeUtil.safelyDisposeChange(change);
                        }
                        if (undo != null)
                        {
                            if (undos != null)
                                undos.add(undo);
                            else
                                ChangeUtil.safelyDisposeChange(undo);
                        }
                        else if (undos != null)
                        {
                            ChangeUtil.safelyDisposeChanges(undos);
                            undos = null;
                        }
                    }
                    if (undos == null)
                        return null;
                    Collections.reverse(undos);
                    Change result = new WorkspaceEditUndoChange(name, undos);
                    undos = null; // transfer ownership
                    return result;
                }
                finally
                {
                    ChangeUtil.safelyDisposeChanges(undos);
                }
            }

            @Override
            public Object getModifiedElement()
            {
                return null;
            }
        }
    }

    private static class CompositeChangeWithRollback
        extends CompositeChange
    {
        CompositeChangeWithRollback(String name)
        {
            super(name);
        }

        @Override
        public Change perform(IProgressMonitor pm) throws CoreException
        {
            try
            {
                return super.perform(pm);
            }
            catch (OperationCanceledException e)
            {
                rollback();
                throw e;
            }
            catch (Exception | LinkageError | AssertionError e) // same as in SafeRunner
            {
                Activator.logError(e);

                Boolean rollbackResult = rollback();

                throw newChangeExecutionFailedException(getName(), e, rollbackResult);
            }
        }

        @Override
        public Change getUndoUntilException()
        {
            return null;
        }

        private Boolean rollback()
        {
            Change undo = super.getUndoUntilException();
            if (undo == null)
                return null;
            try
            {
                undo.initializeValidationData(new NullProgressMonitor());
                return WorkspaceEditChangeFactory.rollback(Collections.singletonList(undo));
            }
            finally
            {
                ChangeUtil.safelyDisposeChange(undo);
            }
        }
    }

    private static class WorkspaceEditUndoChange
        extends Change
    {
        private final String name;
        private final List<Change> changes;

        // changes are assumed to have been initialized
        WorkspaceEditUndoChange(String name, List<Change> changes)
        {
            this.name = Objects.requireNonNull(name);
            this.changes = Objects.requireNonNull(changes);
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public void dispose()
        {
            ChangeUtil.safelyDisposeChanges(changes);
        }

        @Override
        public void initializeValidationData(IProgressMonitor pm)
        {
        }

        @Override
        public RefactoringStatus isValid(IProgressMonitor pm)
            throws CoreException, OperationCanceledException
        {
            if (changes.isEmpty())
                return new RefactoringStatus();
            return changes.get(0).isValid(pm);
        }

        @Override
        public Change perform(IProgressMonitor pm) throws CoreException
        {
            List<Change> undos = new ArrayList<>();
            try
            {
                SubMonitor subMonitor = SubMonitor.convert(pm, changes.size());
                Iterator<Change> it = changes.iterator();
                while (it.hasNext())
                {
                    Change change = it.next();
                    Change undo =
                        ChangeUtil.executeChange(change, false, undos != null, subMonitor.split(1));
                    if (undo != null)
                    {
                        if (undos != null)
                            undos.add(undo);
                        else
                            ChangeUtil.safelyDisposeChange(undo);
                    }
                    else if (undos != null)
                    {
                        ChangeUtil.safelyDisposeChanges(undos);
                        undos = null;
                    }
                    it.remove();
                    ChangeUtil.safelyDisposeChange(change);
                }
                if (undos == null)
                    return null;
                Collections.reverse(undos);
                Change result = new WorkspaceEditUndoChange(name, undos);
                undos = null; // transfer ownership
                return result;
            }
            finally
            {
                ChangeUtil.safelyDisposeChanges(undos);
            }
        }

        @Override
        public Object getModifiedElement()
        {
            return null;
        }
    }
}
