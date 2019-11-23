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

import org.eclipse.handly.model.ISourceFile;
import org.eclipse.handly.ui.texteditor.SourceFileDocumentProvider;
import org.eclipse.ui.IEditorInput;
import org.lxtk.lx4e.examples.typescript.TypeScriptInputElementProvider;

/**
 * TODO JavaDoc
 */
public class TypeScriptSourceFileDocumentProvider
    extends SourceFileDocumentProvider
{
    @Override
    protected ISourceFile getSourceFile(Object element)
    {
        if (!(element instanceof IEditorInput))
            return null;
        return TypeScriptInputElementProvider.INSTANCE.getElement(
            (IEditorInput)element);
    }
}
