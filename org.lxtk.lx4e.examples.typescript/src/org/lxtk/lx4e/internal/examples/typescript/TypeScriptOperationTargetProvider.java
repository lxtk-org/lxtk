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
package org.lxtk.lx4e.internal.examples.typescript;

import java.net.URI;

import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.examples.typescript.TypeScriptCore;
import org.lxtk.lx4e.examples.typescript.TypeScriptInputElementProvider;
import org.lxtk.lx4e.model.ILanguageSourceFile;

/**
 * TODO JavaDoc
 */
public class TypeScriptOperationTargetProvider
{
    /**
     * TODO JavaDoc
     *
     * @param editor may be <code>null</code>
     * @return the operation target, or <code>null</code> if none
     */
    public static LanguageOperationTarget getOperationTarget(IEditorPart editor)
    {
        if (editor == null)
            return null;

        ILanguageSourceFile sourceFile =
            TypeScriptInputElementProvider.INSTANCE.getElement(
                editor.getEditorInput());
        if (sourceFile == null)
            return null;

        URI documentUri = sourceFile.getDocumentUri();
        if (documentUri == null)
            return null;

        return new LanguageOperationTarget(documentUri, TypeScriptCore.LANG_ID,
            TypeScriptCore.LANG_SERVICE);
    }

    private TypeScriptOperationTargetProvider()
    {
    }
}
