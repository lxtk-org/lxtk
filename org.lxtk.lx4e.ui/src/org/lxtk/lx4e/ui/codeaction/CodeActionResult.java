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

import java.util.List;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Represents the result of a code action request.
 */
public class CodeActionResult
{
    private final List<Either<Command, CodeAction>> codeActions;

    /**
     * Constructor.
     *
     * @param codeActions may be <code>null</code>
     */
    public CodeActionResult(List<Either<Command, CodeAction>> codeActions)
    {
        this.codeActions = codeActions;
    }

    /**
     * Returns the code action list.
     *
     * @return the code action list, or <code>null</code> if none
     */
    public List<Either<Command, CodeAction>> getCodeActions()
    {
        return codeActions;
    }
}
