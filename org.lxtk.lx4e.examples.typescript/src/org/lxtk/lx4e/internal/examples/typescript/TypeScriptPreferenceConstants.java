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

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Provides preference constants for TypeScript.
 */
public class TypeScriptPreferenceConstants
{
    public static final String EDITOR_MATCHING_BRACKETS = "matchingBrackets"; //$NON-NLS-1$
    public static final String EDITOR_MATCHING_BRACKETS_COLOR =
        "matchingBracketsColor"; //$NON-NLS-1$

    public static void initializeDefaultValues(IPreferenceStore store)
    {
        store.setDefault(EDITOR_MATCHING_BRACKETS, true);
        store.setDefault(EDITOR_MATCHING_BRACKETS_COLOR, "128,128,128"); //$NON-NLS-1$
    }

    private TypeScriptPreferenceConstants()
    {
    }
}
