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
package org.lxtk.lx4e.internal.examples.json;

import org.eclipse.core.resources.IFile;
import org.eclipse.handly.model.impl.support.IModelManager;
import org.lxtk.LanguageService;
import org.lxtk.Workspace;
import org.lxtk.lx4e.examples.json.JsonCore;
import org.lxtk.lx4e.model.impl.LanguageElement;
import org.lxtk.lx4e.model.impl.LanguageSourceFile;

/**
 * TODO JavaDoc
 */
public class JsonSourceFile
    extends LanguageSourceFile
{
    /**
     * TODO JavaDoc
     *
     * @param parent may be <code>null</code>
     * @param file not <code>null</code>
     */
    public JsonSourceFile(LanguageElement parent, IFile file)
    {
        super(parent, file, JsonCore.LANG_ID);
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
