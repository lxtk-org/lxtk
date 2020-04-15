/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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

import static org.eclipse.ltk.core.refactoring.RefactoringStatus.createFatalErrorStatus;
import static org.lxtk.lx4e.uris.UriHandlers.compose;
import static org.lxtk.lx4e.uris.UriHandlers.getBuffer;
import static org.lxtk.lx4e.uris.UriHandlers.toDisplayString;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.snapshot.DocumentSnapshot;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.NonExpiringSnapshot;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.lxtk.DocumentUri;
import org.lxtk.TextDocument;
import org.lxtk.TextDocumentSnapshot;
import org.lxtk.Workspace;
import org.lxtk.lx4e.DocumentUtil;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.lx4e.uris.EfsUriHandler;
import org.lxtk.lx4e.uris.IUriHandler;
import org.lxtk.lx4e.uris.ResourceUriHandler;
import org.lxtk.lx4e.uris.TextDocumentUriHandler;

/**
 * Creates a {@link Change} object for a given {@link WorkspaceEdit}.
 */
public class WorkspaceEditChangeFactory
{
    /**
     * The associated {@link Workspace} (never <code>null</code>).
     */
    protected final Workspace workspace;
    /**
     * The associated {@link IUriHandler} (never <code>null</code>).
     */
    protected final IUriHandler uriHandler;

    /**
     * Creates a new factory instance with the given {@link Workspace} and a
     * default {@link IUriHandler}.
     *
     * @param workspace not <code>null</code>
     */
    public WorkspaceEditChangeFactory(Workspace workspace)
    {
        this(workspace, compose(new TextDocumentUriHandler(workspace),
            new ResourceUriHandler(), new EfsUriHandler()));
    }

    /**
     * Creates a new factory instance with the given {@link Workspace}
     * and the given {@link IUriHandler}.
     *
     * @param workspace not <code>null</code>
     * @param uriHandler not <code>null</code>
     */
    public WorkspaceEditChangeFactory(Workspace workspace,
        IUriHandler uriHandler)
    {
        this.workspace = Objects.requireNonNull(workspace);
        this.uriHandler = Objects.requireNonNull(uriHandler);
    }

    /**
     * Creates a {@link Change} object that performs the workspace transformation
     * described by the given {@link WorkspaceEdit}.
     *
     * @param name the human readable name of the change. Will
     *  be used to display the change in the user interface
     * @param workspaceEdit a {@link WorkspaceEdit} describing the workspace
     *  transformation that will be performed by the change (not <code>null</code>)
     * @param status a {@link RefactoringStatus} that can be used to report
     *  problems detected during creation of the change (not <code>null</code>)
     * @param pm a progress monitor, or <code>null</code>
     *  if progress reporting is not desired. The caller must not rely on
     *  {@link IProgressMonitor#done()} having been called by the receiver
     * @return the created change (never <code>null</code>)
     * @throws CoreException if this method could not create a change
     * @throws OperationCanceledException if this method is canceled
     */
    public Change createChange(String name, WorkspaceEdit workspaceEdit,
        RefactoringStatus status, IProgressMonitor pm) throws CoreException
    {
        CompositeChange change = new CompositeChange(name);
        change.markAsSynthetic();
        List<Either<TextDocumentEdit, ResourceOperation>> documentChanges =
            workspaceEdit.getDocumentChanges();
        if (documentChanges != null)
        {
            SubMonitor monitor = SubMonitor.convert(pm, documentChanges.size());
            for (Either<TextDocumentEdit, ResourceOperation> entry : documentChanges)
            {
                status.merge(addChangeEntry(change, entry));
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
                    status.merge(addChangeEntry(change, entry));
                    monitor.split(1);
                }
            }
        }
        return change;
    }

    private RefactoringStatus addChangeEntry(CompositeChange change,
        Either<TextDocumentEdit, ResourceOperation> entry) throws CoreException
    {
        if (entry.isLeft())
            return addTextChange(change, entry.getLeft());
        if (entry.isRight())
            return addResourceChange(change, entry.getRight());
        return null;
    }

    private RefactoringStatus addChangeEntry(CompositeChange change,
        Entry<String, List<TextEdit>> entry) throws CoreException
    {
        return addTextChange(change, new TextDocumentEdit(
            new VersionedTextDocumentIdentifier(entry.getKey(), null),
            entry.getValue()));
    }

    /**
     * Adds a {@link Change} for the given {@link TextDocumentEdit}
     * to the given {@link CompositeChange}.
     *
     * @param change a <code>CompositeChange</code> (never <code>null</code>)
     * @param textDocumentEdit a <code>TextDocumentEdit</code> (never <code>null</code>)
     * @return a {@link RefactoringStatus}. May be <code>null</code>, which
     *  indicates an OK status
     * @throws CoreException if an error condition is detected that makes it
     *  impossible for the factory to create a change
     */
    protected RefactoringStatus addTextChange(CompositeChange change,
        TextDocumentEdit textDocumentEdit) throws CoreException
    {
        VersionedTextDocumentIdentifier textDocumentId =
            textDocumentEdit.getTextDocument();
        URI uri = DocumentUri.convert(textDocumentId.getUri());

        IDocument document;
        ISnapshot snapshot;
        TextDocument textDocument = workspace.getTextDocument(uri);
        if (textDocument != null)
        {
            TextDocumentSnapshot textDocumentSnapshot =
                textDocument.getLastChange().getSnapshot();
            if (textDocumentId.getVersion() != null
                && textDocumentId.getVersion().intValue() != textDocumentSnapshot.getVersion())
            {
                return createFatalErrorStatus(MessageFormat.format(
                    Messages.WorkspaceEditChangeFactory_Stale_workspace_edit,
                    toDisplayString(uri, uriHandler)));
            }
            document = new Document(textDocumentSnapshot.getText());
            snapshot = new DocumentSnapshot(document);
        }
        else
        {
            try (IBuffer buffer = getBuffer(uri, uriHandler))
            {
                NonExpiringSnapshot nonExpiringSnapshot =
                    new NonExpiringSnapshot(buffer);
                document = new Document(nonExpiringSnapshot.getContents());
                snapshot = nonExpiringSnapshot.getWrappedSnapshot();
            }
        }

        MultiTextEdit edit;
        try
        {
            edit = DocumentUtil.toMultiTextEdit(document,
                textDocumentEdit.getEdits());
        }
        catch (MalformedTreeException | BadLocationException e)
        {
            throw Activator.toCoreException(e);
        }

        TextFileChange textChange = new TextFileChange(toDisplayString(uri,
            uriHandler), uri, uriHandler);
        textChange.setEdit(edit);
        textChange.setBase(snapshot);
        change.add(textChange);
        return null;
    }

    /**
     * Adds a {@link Change} for the given {@link ResourceOperation}
     * to the given {@link CompositeChange}.
     *
     * @param change a <code>CompositeChange</code> (never <code>null</code>)
     * @param operation a <code>ResourceOperation</code> (never <code>null</code>)
     * @return a {@link RefactoringStatus}. May be <code>null</code>, which
     *  indicates an OK status
     * @throws CoreException if an error condition is detected that makes it
     *  impossible for the factory to create a change
     */
    protected RefactoringStatus addResourceChange(CompositeChange change,
        ResourceOperation operation) throws CoreException
    {
        throw new CoreException(Activator.createErrorStatus(
            "Resource operations are currently not supported", null)); //$NON-NLS-1$
    }
}
