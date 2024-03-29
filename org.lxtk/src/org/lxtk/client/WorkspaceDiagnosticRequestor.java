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

import org.lxtk.util.Disposable;

/**
 * Represents a requestor for workspace diagnostics.
 */
public interface WorkspaceDiagnosticRequestor
    extends Disposable
{
    /**
     * Triggers workspace diagnostic pull.
     */
    void triggerWorkspacePull();
}
