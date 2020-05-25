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
package org.lxtk.lx4e.internal.examples.json;

import java.net.URI;

import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.examples.json.JsonCore;
import org.lxtk.lx4e.examples.json.JsonInputElementProvider;
import org.lxtk.lx4e.model.ILanguageSourceFile;

/**
 * Provides JSON-specific operation targets.
 *
 * @see LanguageOperationTarget
 */
public class JsonOperationTargetProvider
{
    /**
     * Returns a JSON-specific operation target for the given editor.
     *
     * @param editor may be <code>null</code>
     * @return the operation target, or <code>null</code> if none
     */
    public static LanguageOperationTarget getOperationTarget(IEditorPart editor)
    {
        if (editor == null)
            return null;

        return getOperationTarget(
            JsonInputElementProvider.INSTANCE.getElement(editor.getEditorInput()));
    }

    private static LanguageOperationTarget getOperationTarget(ILanguageSourceFile sourceFile)
    {
        if (sourceFile == null)
            return null;

        URI documentUri = sourceFile.getDocumentUri();
        if (documentUri == null)
            return null;

        return new LanguageOperationTarget(documentUri, JsonCore.LANG_ID, JsonCore.LANG_SERVICE);
    }

    private JsonOperationTargetProvider()
    {
    }
}
