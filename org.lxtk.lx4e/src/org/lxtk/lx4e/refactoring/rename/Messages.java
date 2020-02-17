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
package org.lxtk.lx4e.refactoring.rename;

import org.eclipse.osgi.util.NLS;

class Messages
    extends NLS
{
    private static final String BUNDLE_NAME =
        "org.lxtk.lx4e.refactoring.rename.messages"; //$NON-NLS-1$

    public static String RenameRefactoring_New_name_is_empty;
    public static String RenameRefactoring_New_name_is_equal_to_current_name;
    public static String RenameRefactoring_No_prepare_rename_result;
    public static String RenameRefactoring_No_rename_provider;
    public static String RenameRefactoring_No_workspace_edit;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
