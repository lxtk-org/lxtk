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

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DiagnosticRegistrationOptions;
import org.eclipse.lsp4j.DocumentDiagnosticParams;
import org.eclipse.lsp4j.DocumentDiagnosticReport;
import org.eclipse.lsp4j.WorkspaceDiagnosticParams;
import org.eclipse.lsp4j.WorkspaceDiagnosticReport;

/**
 * Provides diagnostics for a given text document or the workspace.
 */
public interface DiagnosticProvider
{
    /**
     * Returns registration options for this provider.
     *
     * @return the registration options (never <code>null</code>).
     *  Clients <b>must not</b> modify the returned object
     */
    DiagnosticRegistrationOptions getRegistrationOptions();

    /**
     * Requests diagnostics for the given text document.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     */
    CompletableFuture<DocumentDiagnosticReport> getDocumentDiagnostics(
        DocumentDiagnosticParams params);

    /**
     * Requests diagnostics for the workspace.
     *
     * @param params not <code>null</code>
     * @return result future (never <code>null</code>)
     * @throws UnsupportedOperationException if no support for workspace diagnostics is available
     * @see DiagnosticRegistrationOptions#isWorkspaceDiagnostics()
     */
    CompletableFuture<WorkspaceDiagnosticReport> getWorkspaceDiagnostics(
        WorkspaceDiagnosticParams params);
}
