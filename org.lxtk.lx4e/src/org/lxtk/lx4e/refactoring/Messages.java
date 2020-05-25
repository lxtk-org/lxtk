/*******************************************************************************
 * Copyright (c) 2019 1C-Soft LLC.
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
package org.lxtk.lx4e.refactoring;

import org.eclipse.osgi.util.NLS;

class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.lxtk.lx4e.refactoring.messages"; //$NON-NLS-1$

    public static String TextFileChange_Cannot_apply_stale_change;
    public static String UndoTextFileChange_Cannot_undo_stale_change;
    public static String UndoTextFileChange_File_should_exist;
    public static String UndoTextFileChange_File_should_not_exist;
    public static String WorkspaceEditChangeFactory_Stale_workspace_edit;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
