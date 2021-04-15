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

import org.lxtk.util.EventStream;

/**
 * Represents a source of {@link TextDocumentSaveEvent}s.
 */
public interface TextDocumentSaveEventSource
{
    /**
     * Returns an event emitter firing when a text document is saved.
     *
     * @return an event emitter firing when a text document is saved
     *  (never <code>null</code>)
     */
    EventStream<TextDocumentSaveEvent> onDidSaveTextDocument();
}
