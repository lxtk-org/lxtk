/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC.
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
package org.lxtk.lx4e.internal.examples.json.editor;

import org.eclipse.ui.IEditorPart;
import org.lxtk.LanguageOperationTarget;
import org.lxtk.lx4e.internal.examples.json.JsonOperationTargetProvider;
import org.lxtk.lx4e.ui.format.AbstractFormatHandler;

/**
 * A formatting handler for JSON.
 */
public class FormatHandler
    extends AbstractFormatHandler
{
    @Override
    protected LanguageOperationTarget getLanguageOperationTarget(IEditorPart editor)
    {
        return JsonOperationTargetProvider.getOperationTarget(editor);
    }
}
