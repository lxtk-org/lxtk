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
package org.lxtk.lx4e.ui.highlight;

import org.eclipse.osgi.util.NLS;

class Messages
    extends NLS
{
    private static final String BUNDLE_NAME =
        "org.lxtk.lx4e.ui.highlight.messages"; //$NON-NLS-1$

    public static String Highlighter_Occurrence;
    public static String Highlighter_Write_occurrence;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
