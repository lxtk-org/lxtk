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
package org.lxtk.client;

import org.lxtk.TextDocument;
import org.lxtk.util.Disposable;

/**
 * Represents a requestor for document diagnostics.
 */
public interface DiagnosticRequestor
    extends Disposable
{
    /**
     * Triggers diagnostic pull for the given text document.
     * If no diagnostic pull sequence is currently being tracked for the document,
     * begins tracking a new sequence.
     *
     * @param document not <code>null</code>
     * @param context may be <code>null</code>
     */
    void triggerDocumentPull(TextDocument document, TriggeringContext context);

    /**
     * Attempts to cancel diagnostic pull that was triggered and has not yet been completed
     * for the given text document. Has no effect if there is nothing to cancel.
     *
     * @param document not <code>null</code>
     */
    void cancelDocumentPull(TextDocument document);

    /**
     * Stops tracking the current diagnostic pull sequence for the given text document.
     * If no diagnostic pull sequence is currently being tracked for the document,
     * this method has no effect.
     *
     * @param document not <code>null</code>
     */
    void endDocumentPullSequence(TextDocument document);

    /**
     * Represents a context for triggering diagnostic pull for a document.
     */
    interface TriggeringContext
    {
        /**
         * Returns the reason for triggering diagnostic pull for the document.
         *
         * @return the triggering reason, or <code>null</code> if there is no specific reason
         */
        TriggeringReason getTriggeringReason();
    }

    /**
     * Enumerates possible reasons for triggering diagnostic pull for a document.
     */
    enum TriggeringReason
    {
        /**
         * The document has become active in the UI.
         */
        DID_BECOME_ACTIVE,
        /**
         * The document has become visible in the UI.
         */
        DID_BECOME_VISIBLE,
        /**
         * The content of the document has changed.
         */
        CONTENT_CHANGE,
        /**
         * The content of a related document has changed.
         */
        INTERFILE_CHANGE
    }
}
