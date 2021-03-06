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
package org.lxtk.lx4e.internal.examples.typescript.editor;

import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.DocumentService;
import org.lxtk.lx4e.examples.typescript.TypeScriptCore;
import org.lxtk.lx4e.internal.examples.typescript.TypeScriptOperationTargetProvider;
import org.lxtk.lx4e.ui.references.AbstractFindReferencesHandler;
import org.lxtk.lx4e.ui.references.ReferenceSearchQuery;

/**
 * A handler that creates and runs a {@link ReferenceSearchQuery} for TypeScript.
 */
public class FindReferencesHandler
    extends AbstractFindReferencesHandler
{
    @Override
    protected LanguageOperationTarget getLanguageOperationTarget(IEditorPart editor)
    {
        return TypeScriptOperationTargetProvider.getOperationTarget(editor);
    }

    @Override
    protected DocumentService getDocumentService()
    {
        return TypeScriptCore.DOCUMENT_SERVICE;
    }
}
