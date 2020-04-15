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
package org.lxtk.lx4e.util;

/**
 * The default {@link WordFinder}.
  *
 * @noextend This class is not intended to be subclassed by clients.
 *  Extend {@link WordFinder} if you need to specialize the default behavior.
 * @noinstantiate This class is not intended to be instantiated by clients.
 *  Use the provided {@link #INSTANCE}.
*/
public class DefaultWordFinder
    extends WordFinder
{
    /**
     * The default instance of the word finder.
     */
    public static final WordFinder INSTANCE = new DefaultWordFinder();

    private DefaultWordFinder()
    {
    }
}
