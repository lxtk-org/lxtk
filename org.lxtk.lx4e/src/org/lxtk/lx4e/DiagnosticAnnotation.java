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
package org.lxtk.lx4e;

import java.util.Objects;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.lsp4j.Diagnostic;

/**
 * TODO JavaDoc
 */
public class DiagnosticAnnotation
    extends Annotation
    implements IDiagnosticAnnotation
{
    private final Diagnostic diagnostic;
    private boolean isQuickFixable, isQuickFixableStateSet;

    /**
     * TODO JavaDoc
     *
     * @param diagnostic not <code>null</code>
     */
    public DiagnosticAnnotation(Diagnostic diagnostic)
    {
        this.diagnostic = Objects.requireNonNull(diagnostic);
    }

    @Override
    public Diagnostic getDiagnostic()
    {
        return diagnostic;
    }

    @Override
    public String getText()
    {
        return diagnostic.getMessage();
    }

    @Override
    public String getType()
    {
        switch (diagnostic.getSeverity())
        {
        case Error:
            return "org.eclipse.ui.workbench.texteditor.error"; //$NON-NLS-1$
        case Warning:
            return "org.eclipse.ui.workbench.texteditor.warning"; //$NON-NLS-1$
        default:
            return "org.eclipse.ui.workbench.texteditor.info"; //$NON-NLS-1$
        }
    }

    @Override
    public void setQuickFixable(boolean state)
    {
        isQuickFixable = state;
        isQuickFixableStateSet = true;
    }

    @Override
    public boolean isQuickFixableStateSet()
    {
        return isQuickFixableStateSet;
    }

    @Override
    public boolean isQuickFixable() throws AssertionFailedException
    {
        Assert.isTrue(isQuickFixableStateSet);
        return isQuickFixable;
    }
}
