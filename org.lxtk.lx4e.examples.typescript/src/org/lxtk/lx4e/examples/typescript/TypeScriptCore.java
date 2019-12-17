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
package org.lxtk.lx4e.examples.typescript;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.handly.model.IElementChangeListener;
import org.lxtk.CommandService;
import org.lxtk.DefaultCommandService;
import org.lxtk.DefaultWorkspace;
import org.lxtk.LanguageService;
import org.lxtk.Workspace;
import org.lxtk.lx4e.EclipseLanguageService;
import org.lxtk.lx4e.internal.examples.typescript.ModelManager;
import org.lxtk.lx4e.internal.examples.typescript.TypeScriptSourceFile;
import org.lxtk.lx4e.model.ILanguageElement;
import org.lxtk.lx4e.model.ILanguageSourceFile;

/**
 * TODO JavaDoc
 */
public class TypeScriptCore
{
    public static final Workspace WORKSPACE = new DefaultWorkspace();

    public static final CommandService CMD_SERVICE =
        new DefaultCommandService();

    public static final LanguageService LANG_SERVICE =
        new EclipseLanguageService();

    public static final String LANG_ID = "typescript"; //$NON-NLS-1$

    /**
     * Returns the element corresponding to the given resource,
     * or <code>null</code> if unable to associate the given resource
     * with an element.
     *
     * @param resource the given resource (may be <code>null</code>)
     * @return the element corresponding to the given resource,
     *  or <code>null</code> if unable to associate the given resource
     *  with an element
     */
    public static ILanguageElement create(IResource resource)
    {
        if (resource instanceof IFile)
            return createSourceFileFrom((IFile)resource);
        return null;

    }

    /**
     * Returns the source file element corresponding to the given file,
     * or <code>null</code> if unable to associate the given file with a
     * source file element.
     *
     * @param file the given file (may be <code>null</code>)
     * @return the source file element corresponding to the given file,
     *  or <code>null</code> if unable to associate the given file with a
     *  source file element
     */
    public static ILanguageSourceFile createSourceFileFrom(IFile file)
    {
        if (file == null)
            return null;
        String fileExtension = file.getFileExtension();
        if ("ts".equals(fileExtension) || "js".equals(fileExtension)) //$NON-NLS-1$ //$NON-NLS-2$
            return new TypeScriptSourceFile(null, file);
        return null;
    }

    /**
     * Adds the given listener for changes to elements in the language model.
     * Has no effect if an identical listener is already registered.
     * <p>
     * Once registered, a listener starts receiving notification of changes to
     * elements in the language model. The listener continues to receive
     * notifications until it is removed.
     * </p>
     *
     * @param listener the listener (not <code>null</code>)
     * @see #removeElementChangeListener(IElementChangeListener)
     */
    public static void addElementChangeListener(IElementChangeListener listener)
    {
        ModelManager.INSTANCE.getNotificationManager().addElementChangeListener(
            listener);
    }

    /**
     * Removes the given element change listener.
     * Has no effect if an identical listener is not registered.
     *
     * @param listener the listener (not <code>null</code>)
     */
    public static void removeElementChangeListener(
        IElementChangeListener listener)
    {
        ModelManager.INSTANCE.getNotificationManager().removeElementChangeListener(
            listener);
    }

    private TypeScriptCore()
    {
    }
}
