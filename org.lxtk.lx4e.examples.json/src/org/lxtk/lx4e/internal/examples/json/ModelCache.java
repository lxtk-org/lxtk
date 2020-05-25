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

import java.util.HashMap;

import org.eclipse.handly.model.IElement;
import org.eclipse.handly.model.impl.support.ElementCache;
import org.eclipse.handly.model.impl.support.IBodyCache;
import org.lxtk.lx4e.model.ILanguageSourceFile;
import org.lxtk.lx4e.model.ILanguageSymbol;

class ModelCache
    implements IBodyCache
{
    private static final int DEFAULT_FILE_SIZE = 250;
    private static final int DEFAULT_CHILDREN_SIZE = DEFAULT_FILE_SIZE * 20; // average 20 children per file

    // The memory ratio that should be applied to the above constants.
    private final double memoryRatio = getMemoryRatio();

    private ElementCache fileCache;
    private HashMap<IElement, Object> symbolCache;

    ModelCache()
    {
        // set the size of the caches as a function of the maximum amount of memory available
        fileCache = new ElementCache((int)(DEFAULT_FILE_SIZE * memoryRatio));
        symbolCache = new HashMap<>((int)(DEFAULT_CHILDREN_SIZE * memoryRatio));
    }

    @Override
    public Object get(IElement element)
    {
        if (element instanceof ILanguageSourceFile)
            return fileCache.get(element);

        return symbolCache.get(element);
    }

    @Override
    public Object peek(IElement element)
    {
        if (element instanceof ILanguageSourceFile)
            return fileCache.peek(element);

        return symbolCache.get(element);
    }

    @Override
    public void put(IElement element, Object body)
    {
        if (element instanceof ILanguageSourceFile)
            fileCache.put(element, body);
        else if (element instanceof ILanguageSymbol)
            symbolCache.put(element, body);
    }

    @Override
    public void remove(IElement element)
    {
        if (element instanceof ILanguageSourceFile)
            fileCache.remove(element);
        else
            symbolCache.remove(element);
    }

    private double getMemoryRatio()
    {
        long maxMemory = Runtime.getRuntime().maxMemory();
        // if max memory is infinite, set the ratio to 4d
        // which corresponds to the 256MB that Eclipse defaults to
        // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=111299)
        return maxMemory == Long.MAX_VALUE ? 4d : ((double)maxMemory) / (64 * 0x100000); // 64MB is the base memory for most JVM
    }
}
