/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.lxtk.FileCreate;
import org.lxtk.FileCreateEvent;
import org.lxtk.FileCreateEventSource;
import org.lxtk.FileDelete;
import org.lxtk.FileDeleteEvent;
import org.lxtk.FileDeleteEventSource;
import org.lxtk.FileRename;
import org.lxtk.FileRenameEvent;
import org.lxtk.FileRenameEventSource;
import org.lxtk.FileWillCreateEventSource;
import org.lxtk.FileWillDeleteEventSource;
import org.lxtk.FileWillRenameEventSource;
import org.lxtk.lx4e.IWorkspaceEditChangeFactory;
import org.lxtk.lx4e.internal.Activator;
import org.lxtk.lx4e.requests.Request;
import org.lxtk.util.EventEmitter;
import org.lxtk.util.EventStream;
import org.lxtk.util.WaitUntilEvent;
import org.lxtk.util.WaitUntilEventEmitter;

/**
 * Default implementation of {@link IFileOperationParticipantSupport}.
 */
public class FileOperationParticipantSupport
    implements FileWillCreateEventSource, FileWillDeleteEventSource, FileWillRenameEventSource,
    FileCreateEventSource, FileDeleteEventSource, FileRenameEventSource,
    IFileOperationParticipantSupport
{
    private final IWorkspaceEditChangeFactory changeFactory;
    private final WaitUntilEventEmitter<FileCreateEvent, WorkspaceEdit> onWillCreateFiles =
        new WaitUntilEventEmitter<>();
    private final WaitUntilEventEmitter<FileDeleteEvent, WorkspaceEdit> onWillDeleteFiles =
        new WaitUntilEventEmitter<>();
    private final WaitUntilEventEmitter<FileRenameEvent, WorkspaceEdit> onWillRenameFiles =
        new WaitUntilEventEmitter<>();
    private final EventEmitter<FileCreateEvent> onDidCreateFiles = new EventEmitter<>();
    private final EventEmitter<FileDeleteEvent> onDidDeleteFiles = new EventEmitter<>();
    private final EventEmitter<FileRenameEvent> onDidRenameFiles = new EventEmitter<>();

    /**
     * Constructor.
     *
     * @param changeFactory not <code>null</code>
     */
    public FileOperationParticipantSupport(IWorkspaceEditChangeFactory changeFactory)
    {
        this.changeFactory = Objects.requireNonNull(changeFactory);
    }

    @Override
    public Change computePreCreateChange(List<FileCreate> files, IProgressMonitor monitor)
        throws CoreException
    {
        Request<List<WorkspaceEdit>> request = new Request<>()
        {
            @Override
            protected CompletableFuture<List<WorkspaceEdit>> send()
            {
                return onWillCreateFiles.emit(new FileCreateEvent(files), Activator.LOGGER);
            }
        };
        request.setTitle(Messages.FileOperationParticipantSupport_Computing_pre_create_changes);
        request.setTimeout(getTimeout());
        request.setProgressMonitor(monitor);
        request.setMayThrow(false);

        List<WorkspaceEdit> workspaceEdits = request.sendAndReceive();
        if (workspaceEdits == null || workspaceEdits.isEmpty())
            return null;

        CompositeChange result = new CompositeChange("Pre-create changes"); //$NON-NLS-1$
        result.markAsSynthetic();
        for (WorkspaceEdit workspaceEdit : workspaceEdits)
        {
            result.add(changeFactory.createChange("Pre-create change", workspaceEdit, null)); //$NON-NLS-1$
        }
        return result;
    }

    @Override
    public Change computePreDeleteChange(List<FileDelete> files, IProgressMonitor monitor)
        throws CoreException
    {
        Request<List<WorkspaceEdit>> request = new Request<>()
        {
            @Override
            protected CompletableFuture<List<WorkspaceEdit>> send()
            {
                return onWillDeleteFiles.emit(new FileDeleteEvent(files), Activator.LOGGER);
            }
        };
        request.setTitle(Messages.FileOperationParticipantSupport_Computing_pre_delete_changes);
        request.setTimeout(getTimeout());
        request.setProgressMonitor(monitor);
        request.setMayThrow(false);

        List<WorkspaceEdit> workspaceEdits = request.sendAndReceive();
        if (workspaceEdits == null || workspaceEdits.isEmpty())
            return null;

        CompositeChange result = new CompositeChange("Pre-delete changes"); //$NON-NLS-1$
        result.markAsSynthetic();
        for (WorkspaceEdit workspaceEdit : workspaceEdits)
        {
            result.add(changeFactory.createChange("Pre-delete change", workspaceEdit, null)); //$NON-NLS-1$
        }
        return result;
    }

    @Override
    public Change computePreRenameChange(List<FileRename> files, IProgressMonitor monitor)
        throws CoreException
    {
        Request<List<WorkspaceEdit>> request = new Request<>()
        {
            @Override
            protected CompletableFuture<List<WorkspaceEdit>> send()
            {
                return onWillRenameFiles.emit(new FileRenameEvent(files), Activator.LOGGER);
            }
        };
        request.setTitle(Messages.FileOperationParticipantSupport_Computing_pre_rename_changes);
        request.setTimeout(getTimeout());
        request.setProgressMonitor(monitor);
        request.setMayThrow(false);

        List<WorkspaceEdit> workspaceEdits = request.sendAndReceive();
        if (workspaceEdits == null || workspaceEdits.isEmpty())
            return null;

        CompositeChange result = new CompositeChange("Pre-rename changes"); //$NON-NLS-1$
        result.markAsSynthetic();
        for (WorkspaceEdit workspaceEdit : workspaceEdits)
        {
            result.add(changeFactory.createChange("Pre-rename change", workspaceEdit, null)); //$NON-NLS-1$
        }
        return result;
    }

    @Override
    public Change computePostCreateChange(List<FileCreate> files, IProgressMonitor monitor)
        throws CoreException
    {
        CompositeChange result = new CompositeChange("Post-create changes") //$NON-NLS-1$
        {
            @Override
            public Change perform(IProgressMonitor pm) throws CoreException
            {
                onDidCreateFiles.emit(new FileCreateEvent(files), Activator.LOGGER);
                return new NullChange(getName());
            }
        };
        result.markAsSynthetic();
        return result;
    }

    @Override
    public Change computePostDeleteChange(List<FileDelete> files, IProgressMonitor monitor)
        throws CoreException
    {
        CompositeChange result = new CompositeChange("Post-delete changes") //$NON-NLS-1$
        {
            @Override
            public Change perform(IProgressMonitor pm) throws CoreException
            {
                onDidDeleteFiles.emit(new FileDeleteEvent(files), Activator.LOGGER);
                return new NullChange(getName());
            }
        };
        result.markAsSynthetic();
        return result;
    }

    @Override
    public Change computePostRenameChange(List<FileRename> files, IProgressMonitor monitor)
        throws CoreException
    {
        CompositeChange result = new CompositeChange("Post-rename changes") //$NON-NLS-1$
        {
            @Override
            public Change perform(IProgressMonitor pm) throws CoreException
            {
                onDidRenameFiles.emit(new FileRenameEvent(files), Activator.LOGGER);
                return new NullChange(getName());
            }
        };
        result.markAsSynthetic();
        return result;
    }

    @Override
    public EventStream<WaitUntilEvent<FileCreateEvent, WorkspaceEdit>> onWillCreateFiles()
    {
        return onWillCreateFiles;
    }

    @Override
    public EventStream<WaitUntilEvent<FileDeleteEvent, WorkspaceEdit>> onWillDeleteFiles()
    {
        return onWillDeleteFiles;
    }

    @Override
    public EventStream<WaitUntilEvent<FileRenameEvent, WorkspaceEdit>> onWillRenameFiles()
    {
        return onWillRenameFiles;
    }

    @Override
    public EventStream<FileCreateEvent> onDidCreateFiles()
    {
        return onDidCreateFiles;
    }

    @Override
    public EventStream<FileDeleteEvent> onDidDeleteFiles()
    {
        return onDidDeleteFiles;
    }

    @Override
    public EventStream<FileRenameEvent> onDidRenameFiles()
    {
        return onDidRenameFiles;
    }

    /**
     * Returns the timeout for computing a change.
     *
     * @return a positive duration
     */
    protected Duration getTimeout()
    {
        return Duration.ofSeconds(2);
    }
}
