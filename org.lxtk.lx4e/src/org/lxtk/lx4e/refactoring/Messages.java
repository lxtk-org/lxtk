/*******************************************************************************
 * Copyright (c) 2019, 2021 1C-Soft LLC.
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
    public static String WorkspaceEditChangeFactory_Change_execution_failed;
    public static String WorkspaceEditChangeFactory_Change_execution_failed_and_could_not_be_rolled_back;
    public static String WorkspaceEditChangeFactory_Change_execution_failed_and_has_been_rolled_back;
    public static String WorkspaceEditChangeFactory_File_does_not_exist;
    public static String WorkspaceEditChangeFactory_Stale_workspace_edit;
    public static String WorkspaceEditChangeFactory_Text_edit_group_default;
    public static String WorkspaceEditChangeFactory_Text_edit_group_exact;
    public static String WorkspaceEditChangeFactory_Text_edit_group_potential;
    public static String WorkspaceEditChangeFactory_Unsupported_create_operation;
    public static String WorkspaceEditChangeFactory_Unsupported_delete_operation;
    public static String WorkspaceEditChangeFactory_Unsupported_rename_operation;
    public static String WorkspaceEditChangeFactory_Unsupported_resource_operation_kind;
    public static String WorkspaceEditRefactoring_Needs_confirmation;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
