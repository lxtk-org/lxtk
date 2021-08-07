/*******************************************************************************
 * Copyright (c) 2021 1C-Soft LLC.
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
package org.lxtk.util.completion.snippet;

/**
 * Represents the environment in which a snippet is evaluated.
 */
public interface SnippetContext
{
    /**
     * Returns whether the given variable is known (i.e., its name is defined) in this context.
     *
     * @param name the variable name (not <code>null</code>)
     * @return <code>true</code> if the variable is known, and <code>false</code> otherwise
     */
    default boolean isKnownVariable(String name)
    {
        return StandardVariableNames.contains(name);
    }

    /**
     * Returns the value of the given variable set in this context.
     *
     * @param name the variable name (not <code>null</code>)
     * @return the value of the variable, or <code>null</code> if the variable is not set
     */
    String resolveVariable(String name);

    /**
     * Returns the value of the given variable, or the given default value if the variable
     * is not set in this context.
     *
     * @param name the variable name (not <code>null</code>)
     * @param defaultValue may be <code>null</code>
     * @return the value of the variable, or the default value if the variable is not set
     */
    default String resolveVariable(String name, String defaultValue)
    {
        String value = resolveVariable(name);
        if (value == null)
            value = defaultValue;
        return value;
    }
}
