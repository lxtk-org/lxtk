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
package org.lxtk.lx4e.internal.examples.typescript;

import java.util.Objects;
import java.util.function.Supplier;

import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.refactoring.WorkspaceEditChangeFactory;
import org.lxtk.lx4e.ui.codeaction.AbstractQuickAssistProcessor;

/**
 * TypeScript-specific extension of {@link AbstractQuickAssistProcessor}.
 */
public class TypeScriptQuickAssistProcessor
    extends AbstractQuickAssistProcessor
{
    private final Supplier<LanguageOperationTarget> targetSupplier;

    /**
     * Constructor.
     *
     * @param targetSupplier not <code>null</code>
     */
    public TypeScriptQuickAssistProcessor(Supplier<LanguageOperationTarget> targetSupplier)
    {
        this.targetSupplier = Objects.requireNonNull(targetSupplier);
    }

    @Override
    protected LanguageOperationTarget getLanguageOperationTarget()
    {
        return targetSupplier.get();
    }

    @Override
    protected WorkspaceEditChangeFactory getWorkspaceEditChangeFactory()
    {
        return TypeScriptWorkspaceEditChangeFactory.INSTANCE;
    }
}
