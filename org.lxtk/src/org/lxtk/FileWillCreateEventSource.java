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
package org.lxtk;

import org.eclipse.lsp4j.WorkspaceEdit;
import org.lxtk.util.EventStream;
import org.lxtk.util.WaitUntilEvent;

/**
 * Represents a source of events that are emitted when files are going to be created as a result of
 * a refactoring operation within the client.
 */
public interface FileWillCreateEventSource
{
    /**
     * Returns a stream of events that are emitted when files are going to be created as a result of
     * a refactoring operation within the client.
     * <p>
     * An event consumer can asynchronously compute a workspace edit that will be applied to the
     * workspace before the files are created. A future representing the computation result needs
     * to be passed to the event's {@link WaitUntilEvent#accept(java.util.concurrent.CompletableFuture)
     * accept} method. Workspace edits computed by consumers of the event must not conflict with
     * each other.
     * </p>
     *
     * @return a stream of events that are emitted when files are going to be created
     *  (never <code>null</code>)
     */
    EventStream<WaitUntilEvent<FileCreateEvent, WorkspaceEdit>> onWillCreateFiles();
}
