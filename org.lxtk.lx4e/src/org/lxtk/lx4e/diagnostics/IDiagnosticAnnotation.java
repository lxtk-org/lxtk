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
package org.lxtk.lx4e.diagnostics;

import org.eclipse.jface.text.quickassist.IQuickFixableAnnotation;
import org.eclipse.lsp4j.Diagnostic;

/**
 * Interface of annotations that represent LSP diagnostics.
 */
public interface IDiagnosticAnnotation
    extends IQuickFixableAnnotation
{
    /**
     * Returns the associated diagnostic object.
     *
     * @return the associated diagnostic object (never <code>null</code>)
     */
    Diagnostic getDiagnostic();
}
