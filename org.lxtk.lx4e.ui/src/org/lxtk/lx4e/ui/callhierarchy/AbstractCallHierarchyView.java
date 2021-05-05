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
package org.lxtk.lx4e.ui.callhierarchy;

import java.text.MessageFormat;

import org.eclipse.handly.ui.EditorOpener;
import org.eclipse.handly.ui.EditorUtility;
import org.eclipse.handly.ui.callhierarchy.CallHierarchyKind;
import org.eclipse.handly.ui.callhierarchy.CallHierarchyLabelProvider;
import org.eclipse.handly.ui.callhierarchy.CallHierarchyViewPart;
import org.eclipse.handly.ui.callhierarchy.ICallHierarchyNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.ui.IEditorPart;
import org.lxtk.lx4e.ui.DefaultEditorHelper;

/**
 * Partial implementation of a call hierarchy view. The view expects input elements of type
 * {@link CallHierarchyElement}.
 */
public abstract class AbstractCallHierarchyView
    extends CallHierarchyViewPart
{
    @Override
    protected boolean isPossibleInputElement(Object element)
    {
        return element instanceof CallHierarchyElement;
    }

    @Override
    protected ICallHierarchyNode[] createHierarchyRoots(Object[] inputElements)
    {
        boolean outgoing = getHierarchyKind() == CallHierarchyKind.CALLEE;
        int length = inputElements.length;
        ICallHierarchyNode[] result = new ICallHierarchyNode[length];
        for (int i = 0; i < length; i++)
        {
            CallHierarchyElement element = (CallHierarchyElement)inputElements[i];
            result[i] = outgoing ? OutgoingCallHierarchyNode.newRootNode(element)
                : IncomingCallHierarchyNode.newRootNode(element);
        }
        return result;
    }

    @Override
    protected String computeContentDescription()
    {
        boolean outgoing = getHierarchyKind() == CallHierarchyKind.CALLEE;
        Object[] elements = getInputElements();
        switch (elements.length)
        {
        case 0:
            return ""; //$NON-NLS-1$
        case 1:
            return MessageFormat.format(
                outgoing ? Messages.AbstractCallHierarchyView_Outgoing_calls_for_1
                    : Messages.AbstractCallHierarchyView_Incoming_calls_for_1,
                getTextLabel(elements[0]));
        case 2:
            return MessageFormat.format(
                outgoing ? Messages.AbstractCallHierarchyView_Outgoing_calls_for_2
                    : Messages.AbstractCallHierarchyView_Incoming_calls_for_2,
                getTextLabel(elements[0]), getTextLabel(elements[1]));
        default:
            return MessageFormat.format(
                outgoing ? Messages.AbstractCallHierarchyView_Outgoing_calls_for_many
                    : Messages.AbstractCallHierarchyView_Incoming_calls_for_many,
                getTextLabel(elements[0]), getTextLabel(elements[1]));
        }
    }

    @Override
    protected void configureHierarchyViewer(TreeViewer viewer)
    {
        super.configureHierarchyViewer(viewer);
        viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
            new CallHierarchyLabelProvider(new CallHierarchyElementLabelProvider())));
    }

    @Override
    protected EditorOpener createEditorOpener()
    {
        return new EditorOpener(getSite().getPage(), new EditorUtility()
        {
            @Override
            public void revealElement(IEditorPart editor, Object element)
            {
                if (element instanceof CallHierarchyElement)
                {
                    CallHierarchyItem callHierarchyItem =
                        ((CallHierarchyElement)element).getCallHierarchyItem();
                    DefaultEditorHelper.INSTANCE.selectTextRange(editor,
                        callHierarchyItem.getSelectionRange());
                }
                else
                {
                    super.revealElement(editor, element);
                }
            }
        });
    }

    @Override
    protected HistoryEntry createHistoryEntry(Object[] inputElements)
    {
        return new HistoryEntry(inputElements)
        {
            @Override
            public ImageDescriptor getImageDescriptor()
            {
                CallHierarchyElement element = (CallHierarchyElement)inputElements[0];
                return element.getImageDescriptor();
            }

            @Override
            protected String getElementLabel(Object element)
            {
                return getTextLabel(element);
            }
        };
    }

    private static String getTextLabel(Object element)
    {
        if (element instanceof CallHierarchyElement)
            return ((CallHierarchyElement)element).getLabel();

        return ""; //$NON-NLS-1$
    }
}
