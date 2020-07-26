/*******************************************************************************
 * Copyright (c) 2019, 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.hyperlinks;

import org.eclipse.osgi.util.NLS;

class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.lxtk.lx4e.ui.hyperlinks.messages"; //$NON-NLS-1$

    public static String AbstractLocationHyperlinkDetector_Result_label;
    public static String DefinitionHyperlinkDetector_Hyperlink_text;
    public static String DefinitionHyperlinkDetector_Hyperlink_text2;
    public static String DefinitionHyperlinkDetector_Result_label;
    public static String ImplementationHyperlinkDetector_Hyperlink_text;
    public static String ImplementationHyperlinkDetector_Hyperlink_text2;
    public static String ImplementationHyperlinkDetector_Result_label;
    public static String TypeDefinitionHyperlinkDetector_Hyperlink_text;
    public static String TypeDefinitionHyperlinkDetector_Hyperlink_text2;
    public static String TypeDefinitionHyperlinkDetector_Result_label;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
