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
package org.lxtk.lx4e.requests;

import org.eclipse.osgi.util.NLS;

class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.lxtk.lx4e.requests.messages"; //$NON-NLS-1$

    public static String CodeActionRequest_title;
    public static String CompletionRequest_title;
    public static String CompletionResolveRequest_title;
    public static String DefinitionRequest_title;
    public static String DocumentFormattingRequest_title;
    public static String DocumentHighlightRequest_title;
    public static String DocumentRangeFormattingRequest_title;
    public static String DocumentSymbolRequest_title;
    public static String HoverRequest_title;
    public static String PrepareRenameRequest_title;
    public static String ReferencesRequest_title;
    public static String RenameRequest_title;
    public static String Request_Error_occurred;
    public static String Request_Error_occurred__0;
    public static String Request_Timeout_occurred__0;
    public static String Request_Timeout_occurred__0__1;
    public static String SignatureHelpRequest_title;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
