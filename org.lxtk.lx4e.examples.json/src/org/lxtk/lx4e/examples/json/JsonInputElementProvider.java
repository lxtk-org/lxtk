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
package org.lxtk.lx4e.examples.json;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.handly.ui.IInputElementProvider;
import org.eclipse.ui.IEditorInput;
import org.lxtk.lx4e.model.ILanguageSourceFile;

/**
 * JSON-specific implementation of {@link IInputElementProvider}.
 */
public class JsonInputElementProvider
    implements IInputElementProvider
{
    /**
     * The sole instance of the {@link JsonInputElementProvider}.
     */
    public static final JsonInputElementProvider INSTANCE = new JsonInputElementProvider();

    @Override
    public ILanguageSourceFile getElement(IEditorInput editorInput)
    {
        if (editorInput == null)
            return null;

        IFile file = editorInput.getAdapter(IFile.class);
        if (file != null)
            return JsonCore.createSourceFileFrom(file);

        IFileStore fileStore = editorInput.getAdapter(IFileStore.class);
        return JsonCore.createSourceFileFrom(fileStore);
    }

    private JsonInputElementProvider()
    {
    }
}
