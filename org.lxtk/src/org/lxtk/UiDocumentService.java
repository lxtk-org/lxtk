/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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

import java.util.Collection;

import org.lxtk.util.EventStream;

/**
 * Provides support for UI aspects of document management.
 */
public interface UiDocumentService
{
    /**
     * Returns the text document that is currently active in the UI, if any.
     *
     * @return the active text document, or <code>null</code> if none
     */
    TextDocument getActiveTextDocument();

    /**
     * Returns the text documents that are currently visible in the UI.
     *
     * @return the visible text documents (never <code>null</code>, may be empty).
     *  Clients <b>must not</b> modify the returned collection
     */
    Collection<TextDocument> getVisibleTextDocuments();

    /**
     * Returns the text documents that are currently open in the UI.
     *
     * @return the open text documents (never <code>null</code>, may be empty).
     *  Clients <b>must not</b> modify the returned collection
     */
    Collection<TextDocument> getOpenTextDocuments();

    /**
     * Returns a stream of events that are emitted when a text document becomes active in the UI.
     *
     * @return a stream of events that are emitted when a text document becomes active
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidBecomeActiveTextDocument();

    /**
     * Returns a stream of events that are emitted when a text document becomes inactive in the UI.
     *
     * @return a stream of events that are emitted when a text document becomes inactive
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidBecomeInactiveTextDocument();

    /**
     * Returns a stream of events that are emitted when a text document becomes visible in the UI.
     *
     * @return a stream of events that are emitted when a text document becomes visible
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidBecomeVisibleTextDocument();

    /**
     * Returns a stream of events that are emitted when a text document becomes hidden in the UI.
     *
     * @return a stream of events that are emitted when a text document becomes hidden
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidBecomeHiddenTextDocument();

    /**
     * Returns a stream of events that are emitted when a text document gets opened in the UI.
     *
     * @return a stream of events that are emitted when a text document gets opened
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidOpenTextDocument();

    /**
     * Returns a stream of events that are emitted when a text document gets closed in the UI.
     *
     * @return a stream of events that are emitted when a text document gets closed
     *  (never <code>null</code>)
     */
    EventStream<TextDocument> onDidCloseTextDocument();
}
