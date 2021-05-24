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
package org.lxtk.lx4e.ui.refactoring.rename;

import java.util.Objects;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension6;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IUndoManagerExtension;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Point;

/**
 * Represents linked editing mode started for a local rename.
 *
 * @see RenameLinkedModeStarter
 */
public class RenameLinkedMode
{
    private final ITextViewer viewer;
    private final LinkedModeModel model;
    private final LinkedPositionGroup group;
    private final IUndoableOperation startingUndoOperation;

    RenameLinkedMode(ITextViewer viewer, LinkedModeModel model, LinkedPositionGroup group)
    {
        this.viewer = Objects.requireNonNull(viewer);
        this.model = Objects.requireNonNull(model);
        this.group = Objects.requireNonNull(group);
        this.startingUndoOperation = getUndoOperation();
    }

    /**
     * Adds <code>listener</code> to the set of listeners that are informed upon state changes.
     *
     * @param listener not <code>null</code>
     */
    public void addLinkingListener(ILinkedModeListener listener)
    {
        model.addLinkingListener(listener);
    }

    /**
     * Removes <code>listener</code> from the set of listeners that are informed upon state changes.
     *
     * @param listener may be <code>null</code>
     */
    public void removeLinkingListener(ILinkedModeListener listener)
    {
        model.removeLinkingListener(listener);
    }

    /**
     * Returns the linked positions. The positions are the actual positions and must not be modified;
     * the array is a copy of internal structures.
     *
     * @return the linked positions in no particular order
     */
    public LinkedPosition[] getLinkedPositions()
    {
        return group.getPositions();
    }

    /**
     * Returns the linked position that includes the current selection.
     *
     * @return the linked position that includes the current selection, or <code>null</code> if none
     */
    public LinkedPosition getCurrentLinkedPosition()
    {
        Point selection = viewer.getSelectedRange();
        int start = selection.x;
        int end = start + selection.y;
        LinkedPosition[] positions = group.getPositions();
        for (LinkedPosition position : positions)
        {
            if (position.includes(start) && position.includes(end))
                return position;
        }
        return null;
    }

    /**
     * Rolls back the text changes made since this linked mode was started.
     */
    public void undoChanges()
    {
        if (viewer instanceof ITextViewerExtension6)
        {
            IUndoManager undoManager = ((ITextViewerExtension6)viewer).getUndoManager();
            if (undoManager instanceof IUndoManagerExtension)
            {
                IUndoManagerExtension undoManagerExtension = (IUndoManagerExtension)undoManager;
                IUndoContext undoContext = undoManagerExtension.getUndoContext();
                IOperationHistory operationHistory = OperationHistoryFactory.getOperationHistory();
                while (undoManager.undoable())
                {
                    if (startingUndoOperation != null && startingUndoOperation.equals(
                        operationHistory.getUndoOperation(undoContext)))
                        break;
                    undoManager.undo();
                }
            }
        }
    }

    /**
     * Exits this linked mode.
     */
    public void exit()
    {
        model.exit(ILinkedModeListener.NONE);
    }

    private IUndoableOperation getUndoOperation()
    {
        if (viewer instanceof ITextViewerExtension6)
        {
            IUndoManager undoManager = ((ITextViewerExtension6)viewer).getUndoManager();
            if (undoManager instanceof IUndoManagerExtension)
            {
                IUndoManagerExtension undoManagerExtension = (IUndoManagerExtension)undoManager;
                IUndoContext undoContext = undoManagerExtension.getUndoContext();
                IOperationHistory operationHistory = OperationHistoryFactory.getOperationHistory();
                return operationHistory.getUndoOperation(undoContext);
            }
        }
        return null;
    }
}
