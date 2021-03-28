/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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
package org.lxtk.lx4e.internal;

import org.eclipse.osgi.util.NLS;

class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.lxtk.lx4e.internal.messages"; //$NON-NLS-1$

    public static String CreateFileChange_name;
    public static String CreateFileChange_Project_is_not_accessible;
    public static String CreateFileChange_Resource_already_exists;
    public static String CreateFileChange_Undo_name;
    public static String CreateFolderChange_name;
    public static String CreateFolderChange_Project_is_not_accesible;
    public static String CreateFolderChange_Undo_name;
    public static String DeleteResourceChange_Directory_is_not_empty;
    public static String DeleteResourceChange_Resource_does_not_exist;
    public static String MoveResourceChange_name;
    public static String MoveResourceChange_Resource_already_exists;
    public static String MoveResourceChange_Resource_does_not_exist;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
