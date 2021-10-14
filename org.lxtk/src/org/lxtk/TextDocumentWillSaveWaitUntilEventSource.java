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

import java.util.List;

import org.eclipse.lsp4j.TextEdit;
import org.lxtk.util.EventStream;
import org.lxtk.util.WaitUntilEvent;

/**
 * Represents a source of events that are emitted when a text document is going to be saved,
 * allowing an event consumer to asynchronously compute text edits that will be applied to the
 * text document before it is saved.
 */
public interface TextDocumentWillSaveWaitUntilEventSource
{
    /**
     * Returns a stream of events that are emitted when a text document is going to be saved.
     * <p>
     * An event consumer can asynchronously compute text edits that will be applied to the text
     * document before it is saved. A future representing the computation result needs to be passed
     * to the event's {@link WaitUntilEvent#accept(java.util.concurrent.CompletableFuture) accept}
     * method. Text edits computed by consumers of the event must not overlap with each other.
     * </p>
     *
     * @return a stream of events that are emitted when a text document is going to be saved
     *  (never <code>null</code>)
     */
    EventStream<WaitUntilEvent<TextDocumentWillSaveEvent,
        List<TextEdit>>> onWillSaveTextDocumentWaitUntil();
}
