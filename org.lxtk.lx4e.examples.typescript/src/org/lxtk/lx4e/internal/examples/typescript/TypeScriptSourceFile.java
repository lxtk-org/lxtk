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
package org.lxtk.lx4e.internal.examples.typescript;

import java.time.Duration;

import org.eclipse.core.resources.IFile;
import org.eclipse.handly.model.impl.support.IModelManager;
import org.lxtk.LanguageService;
import org.lxtk.Workspace;
import org.lxtk.lx4e.examples.typescript.TypeScriptCore;
import org.lxtk.lx4e.model.impl.LanguageElement;
import org.lxtk.lx4e.model.impl.LanguageSourceFile;

/**
 * Represents a TypeScript source file.
 */
public class TypeScriptSourceFile
    extends LanguageSourceFile
{
    /**
     * Constructs a handle for a TypeScript source file with the given
     * parent element and the given workspace file.
     *
     * @param parent the parent of the source file,
     *  or <code>null</code> if the source file has no parent
     * @param file the underlying workspace file (not <code>null</code>)
     */
    public TypeScriptSourceFile(LanguageElement parent, IFile file)
    {
        super(parent, file, TypeScriptCore.LANG_ID);
    }

    @Override
    public IModelManager getModelManager_()
    {
        return ModelManager.INSTANCE;
    }

    @Override
    protected Workspace getWorkspace()
    {
        return TypeScriptCore.WORKSPACE;
    }

    @Override
    protected LanguageService getLanguageService()
    {
        return TypeScriptCore.LANG_SERVICE;
    }

    @Override
    protected Duration getDocumentSymbolTimeout()
    {
        return Duration.ofSeconds(15);
    }
}
