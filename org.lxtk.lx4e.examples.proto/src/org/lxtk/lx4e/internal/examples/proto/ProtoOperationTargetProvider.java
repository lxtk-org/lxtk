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
package org.lxtk.lx4e.internal.examples.proto;

import static org.lxtk.lx4e.examples.proto.ProtoCore.LANGUAGE_ID;
import static org.lxtk.lx4e.examples.proto.ProtoCore.LANGUAGE_SERVICE;

import java.net.URI;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.lxtk.LanguageOperationTarget;

/**
 * Proto operation target provider.
 *
 * @see LanguageOperationTarget
 */
public class ProtoOperationTargetProvider
{
    /**
     * Returns a Proto operation target for the given editor.
     *
     * @param editor may be <code>null</code>
     * @return the operation target, or <code>null</code> if none
     */
    public static LanguageOperationTarget getOperationTarget(IEditorPart editor)
    {
        if (editor == null)
            return null;

        URI documentUri = null;

        IEditorInput editorInput = editor.getEditorInput();
        if (editorInput instanceof IURIEditorInput)
            documentUri = ((IURIEditorInput)editorInput).getURI();

        if (documentUri == null)
            return null;

        return new LanguageOperationTarget(documentUri, LANGUAGE_ID, LANGUAGE_SERVICE);
    }

    private ProtoOperationTargetProvider()
    {
    }
}
