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
package org.lxtk.lx4e.examples.proto;

import org.lxtk.DefaultDocumentService;
import org.lxtk.DefaultWorkspaceService;
import org.lxtk.DocumentService;
import org.lxtk.LanguageService;
import org.lxtk.WorkspaceService;
import org.lxtk.lx4e.EclipseLanguageService;

/**
 * Facade to Proto services.
 */
public class ProtoCore
{
    /**
     * Proto document service.
     */
    public static final DocumentService DOCUMENT_SERVICE = new DefaultDocumentService();

    /**
     * Proto language service.
     */
    public static final LanguageService LANGUAGE_SERVICE = new EclipseLanguageService();

    /**
     * Proto language identifier.
     */
    public static final String LANGUAGE_ID = "proto"; //$NON-NLS-1$

    /**
     * Proto workspace service.
     */
    public static final WorkspaceService WORKSPACE_SERVICE = new DefaultWorkspaceService();

    private ProtoCore()
    {
    }
}
