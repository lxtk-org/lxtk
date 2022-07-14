/*******************************************************************************
 * Copyright (c) 2022 1C-Soft LLC.
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
package org.lxtk.lx4e.ui.typehierarchy;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.handly.ui.EditorOpener;
import org.eclipse.handly.ui.EditorUtility;
import org.eclipse.handly.ui.typehierarchy.TypeHierarchyKind;
import org.eclipse.handly.ui.typehierarchy.TypeHierarchyViewPart;
import org.eclipse.handly.ui.viewer.DeferredTreeContentProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.progress.IElementCollector;
import org.lxtk.lx4e.ui.DefaultEditorHelper;

/**
 * Default implementation of LXTK-based type hierarchy view.
 * The view expects input elements of type {@link TypeHierarchyElement}.
 */
public class DefaultTypeHierarchyView
    extends TypeHierarchyViewPart
{
    /**
     * Constructor.
     */
    public DefaultTypeHierarchyView()
    {
        super(EnumSet.of(TypeHierarchyKind.SUPERTYPES, TypeHierarchyKind.SUBTYPES));
    }

    @Override
    protected boolean isPossibleInputElement(Object element)
    {
        return element instanceof TypeHierarchyElement;
    }

    @Override
    protected String computeContentDescription()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    protected TreeViewer createHierarchyViewer(Composite parent, TypeHierarchyKind kind)
    {
        return new TreeViewer(parent, SWT.MULTI);
    }

    @Override
    protected void configureHierarchyViewer(TreeViewer viewer, TypeHierarchyKind kind)
    {
        viewer.setUseHashlookup(true);
        viewer.setAutoExpandLevel(2);
        viewer.setContentProvider(new TypeHierarchyContentProvider(viewer, kind));
        viewer.setLabelProvider(new TypeHierarchyElementLabelProvider());
    }

    @Override
    protected void setHierarchyViewerInput(TreeViewer viewer, TypeHierarchyKind kind)
    {
        super.setHierarchyViewerInput(viewer, kind);

        Object[] inputElements = getInputElements();
        if (inputElements.length > 0)
            viewer.setSelection(new StructuredSelection(inputElements[0]), true);
    }

    @Override
    protected EditorOpener createEditorOpener()
    {
        return new EditorOpener(getSite().getPage(), new EditorUtility()
        {
            @Override
            public void revealElement(IEditorPart editor, Object element)
            {
                if (element instanceof TypeHierarchyElement)
                {
                    TypeHierarchyItem typeHierarchyItem =
                        ((TypeHierarchyElement)element).getTypeHierarchyItem();
                    DefaultEditorHelper.INSTANCE.selectTextRange(editor,
                        typeHierarchyItem.getSelectionRange());
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
                return ((TypeHierarchyElement)inputElements[0]).getImageDescriptor();
            }

            @Override
            protected String getElementLabel(Object element)
            {
                return ((TypeHierarchyElement)element).getLabel();
            }
        };
    }

    private class TypeHierarchyContentProvider
        extends DeferredTreeContentProvider
    {
        private final TypeHierarchyKind hierarchyKind;

        TypeHierarchyContentProvider(AbstractTreeViewer viewer, TypeHierarchyKind hierarchyKind)
        {
            super(viewer, getViewSite());
            this.hierarchyKind = Objects.requireNonNull(hierarchyKind);
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return getInputElements();
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return getDeferredTreeContentManager().getChildren(parentElement);
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return true;
        }

        @Override
        protected void fetchDeferredChildren(Object parentElement, IElementCollector collector,
            IProgressMonitor monitor)
        {
            if (!(parentElement instanceof TypeHierarchyElement))
                return;

            TypeHierarchyElement element = (TypeHierarchyElement)parentElement;
            TypeHierarchyItem item = element.getTypeHierarchyItem();
            TypeHierarchyUtility utility = element.getTypeHierarchyUtility();

            if (hierarchyKind == TypeHierarchyKind.SUBTYPES)
                utility.fetchSubtypes(item, items -> collect(collector, items, utility), monitor);
            else
                utility.fetchSupertypes(item, items -> collect(collector, items, utility), monitor);

            collector.done();
        }

        private void collect(IElementCollector collector, List<TypeHierarchyItem> items,
            TypeHierarchyUtility utility)
        {
            items.forEach(item -> collector.add(new TypeHierarchyElement(item, utility), null));
        }
    }
}
