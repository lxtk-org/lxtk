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
package org.lxtk.lx4e;

import org.lxtk.TextDocumentChangeEventMergeBuilder;
import org.lxtk.TextDocumentChangeEventMergeStrategy;

/**
 * LX4E-specific implementation of {@link TextDocumentChangeEventMergeStrategy}.
 */
public final class EclipseTextDocumentChangeEventMergeStrategy
    implements TextDocumentChangeEventMergeStrategy
{
    @Override
    public TextDocumentChangeEventMergeBuilder startMerging(String base)
    {
        return new EclipseTextDocumentChangeEventMergeBuilder(base);
    }
}
