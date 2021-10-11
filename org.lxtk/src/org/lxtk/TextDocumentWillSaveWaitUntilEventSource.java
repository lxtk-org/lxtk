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
 * Represents a source of {@link WaitUntilEvent}s that wrap {@link TextDocumentWillSaveEvent}s.
 * Event consumer can asynchronously compute text edits that need to be applied to the text document
 * before it is saved and pass the future representing the computation result to the event's {@link
 * WaitUntilEvent#accept(java.util.concurrent.CompletableFuture) accept} method. Text edits must not
 * overlap with each other.
 */
public interface TextDocumentWillSaveWaitUntilEventSource
{
    /**
     * Returns an event emitter firing when a text document is going to be saved.
     *
     * @return an event emitter firing when a text document is going to be saved
     *  (never <code>null</code>)
     */
    EventStream<WaitUntilEvent<TextDocumentWillSaveEvent,
        List<TextEdit>>> onWillSaveTextDocumentWaitUntil();
}
