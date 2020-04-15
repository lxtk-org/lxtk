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
package org.lxtk.lx4e.internal.examples.json;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.handly.model.impl.support.IModelManager;
import org.lxtk.LanguageService;
import org.lxtk.Workspace;
import org.lxtk.lx4e.examples.json.JsonCore;
import org.lxtk.lx4e.model.impl.LanguageElement;
import org.lxtk.lx4e.model.impl.LanguageSourceFile;

/**
 * Represents a JSON source file.
 */
public class JsonSourceFile
    extends LanguageSourceFile
{
    /**
     * Constructs a handle for a JSON source file with the given parent element
     * and the given workspace file.
     *
     * @param parent the parent of the source file,
     *  or <code>null</code> if the source file has no parent
     * @param file the underlying workspace file (not <code>null</code>)
     */
    public JsonSourceFile(LanguageElement parent, IFile file)
    {
        super(parent, file, JsonCore.LANG_ID);
    }

    /**
     * Constructs a handle for an external JSON source file with the given
     * parent element and the given file store.
     *
     * @param parent may be <code>null</code>
     * @param fileStore not <code>null</code>
     */
    public JsonSourceFile(LanguageElement parent, IFileStore fileStore)
    {
        super(parent, fileStore.toURI(), JsonCore.LANG_ID);
    }

    @Override
    public IModelManager getModelManager_()
    {
        return ModelManager.INSTANCE;
    }

    @Override
    protected Workspace getWorkspace()
    {
        return JsonCore.WORKSPACE;
    }

    @Override
    protected LanguageService getLanguageService()
    {
        return JsonCore.LANG_SERVICE;
    }
}
