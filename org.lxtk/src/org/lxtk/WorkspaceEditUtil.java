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
package org.lxtk;

import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.ChangeAnnotation;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides static utility methods that operate on a {@link WorkspaceEdit}.
 */
public class WorkspaceEditUtil
{
    /**
     * Returns whether the given workspace edit has resource operations.
     *
     * @param workspaceEdit may be <code>null</code>, in which case <code>false</code> is returned
     * @return <code>true</code> if the workspace edit has resource operations, and <code>false</code>
     *  otherwise
     */
    public static boolean hasResourceOperations(WorkspaceEdit workspaceEdit)
    {
        if (workspaceEdit != null)
        {
            List<Either<TextDocumentEdit, ResourceOperation>> documentChanges =
                workspaceEdit.getDocumentChanges();
            if (documentChanges != null)
            {
                for (Either<TextDocumentEdit, ResourceOperation> documentChange : documentChanges)
                {
                    if (documentChange.isRight())
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether the given workspace edit needs to be confirmed by the user.
     *
     * @param workspaceEdit may be <code>null</code>, in which case <code>false</code> is returned
     * @return <code>true</code> if the workspace edit needs confirmation, and <code>false</code>
     *  otherwise
     */
    public static boolean needsConfirmation(WorkspaceEdit workspaceEdit)
    {
        if (workspaceEdit != null)
        {
            Map<String, ChangeAnnotation> changeAnnotations = workspaceEdit.getChangeAnnotations();
            if (changeAnnotations != null)
            {
                for (ChangeAnnotation changeAnnotation : changeAnnotations.values())
                {
                    if (Boolean.TRUE.equals(changeAnnotation.getNeedsConfirmation()))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the change annotation with the given identifier in the given workspace edit.
     *
     * @param workspaceEdit may be <code>null</code>, in which case <code>null</code> is returned
     * @param changeAnnotationId may be <code>null</code>, in which case <code>null</code> is returned
     * @return the corresponding change annotation, or <code>null</code> if none
     */
    public static ChangeAnnotation getChangeAnnotation(WorkspaceEdit workspaceEdit,
        String changeAnnotationId)
    {
        if (workspaceEdit != null)
        {
            Map<String, ChangeAnnotation> changeAnnotations = workspaceEdit.getChangeAnnotations();
            if (changeAnnotations != null)
                return changeAnnotations.get(changeAnnotationId);
        }
        return null;
    }

    private WorkspaceEditUtil()
    {
    }
}
