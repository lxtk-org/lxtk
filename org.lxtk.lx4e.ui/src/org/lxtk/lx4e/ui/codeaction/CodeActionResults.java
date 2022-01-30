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
package org.lxtk.lx4e.ui.codeaction;

import java.util.Map;
import java.util.Objects;

import org.lxtk.CodeActionProvider;

/**
 * Represents a group of {@link CodeActionResult}s.
 */
public class CodeActionResults
{
    private final Map<CodeActionProvider, CodeActionResult> results;

    /**
     * Constructor.
     *
     * @param results not <code>null</code>
     */
    public CodeActionResults(Map<CodeActionProvider, CodeActionResult> results)
    {
        this.results = Objects.requireNonNull(results);
    }

    /**
     * Returns the code action results as a map.
     *
     * @return the code actions results as a map (never <code>null</code>)
     */
    public Map<CodeActionProvider, CodeActionResult> asMap()
    {
        return results;
    }
}
