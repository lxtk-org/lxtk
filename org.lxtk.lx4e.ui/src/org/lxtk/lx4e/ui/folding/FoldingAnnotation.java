/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.folding;

import static org.eclipse.lsp4j.FoldingRangeKind.Comment;
import static org.eclipse.lsp4j.FoldingRangeKind.Imports;
import static org.eclipse.lsp4j.FoldingRangeKind.Region;

import org.eclipse.jface.text.source.projection.ProjectionAnnotation;

/**
 * Represents a folding range.
 */
public class FoldingAnnotation
    extends ProjectionAnnotation
{
    private String kind;

    /**
     * Returns the folding range kind.
     *
     * @return the folding range kind (may be <code>null</code>)
     */
    public String getKind()
    {
        return kind;
    }

    /**
     * Sets the folding range kind.
     *
     * @param kind may be <code>null</code>
     */
    public void setKind(String kind)
    {
        this.kind = normalizeKind(kind);
    }

    private static String normalizeKind(String kind)
    {
        if (kind == null)
            return null;

        switch (kind)
        {
        case Comment:
            return Comment;
        case Imports:
            return Imports;
        case Region:
            return Region;
        default:
            return kind;
        }
    }
}
