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
package org.lxtk.lx4e.ui.callhierarchy;

import org.eclipse.osgi.util.NLS;

class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.lxtk.lx4e.ui.callhierarchy.messages"; //$NON-NLS-1$

    public static String AbstractCallHierarchyView_Incoming_calls_for_1;
    public static String AbstractCallHierarchyView_Incoming_calls_for_2;
    public static String AbstractCallHierarchyView_Incoming_calls_for_many;
    public static String AbstractCallHierarchyView_Outgoing_calls_for_1;
    public static String AbstractCallHierarchyView_Outgoing_calls_for_2;
    public static String AbstractCallHierarchyView_Outgoing_calls_for_many;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
