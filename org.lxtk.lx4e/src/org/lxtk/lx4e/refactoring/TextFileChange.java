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
package org.lxtk.lx4e.refactoring;

import static org.lxtk.lx4e.UriHandlers.getBuffer;
import static org.lxtk.lx4e.UriHandlers.toDisplayString;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.handly.buffer.BufferChange;
import org.eclipse.handly.buffer.IBuffer;
import org.eclipse.handly.buffer.IBufferChange;
import org.eclipse.handly.buffer.SaveMode;
import org.eclipse.handly.snapshot.ISnapshot;
import org.eclipse.handly.snapshot.StaleSnapshotException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;
import org.lxtk.lx4e.IUriHandler;
import org.lxtk.lx4e.internal.Activator;

/**
 * TODO JavaDoc
 */
public final class TextFileChange
    extends TextChange
{
    private final URI uri;
    private final IUriHandler uriHandler;
    private ISnapshot base;
    private SaveMode saveMode = SaveMode.KEEP_SAVED_STATE;
    private IBuffer buffer;

    /**
     * TODO JavaDoc
     *
     * @param name not <code>null</code>
     * @param uri not <code>null</code>
     * @param uriHandler not <code>null</code>
     */
    public TextFileChange(String name, URI uri, IUriHandler uriHandler)
    {
        super(name);
        this.uri = Objects.requireNonNull(uri);
        this.uriHandler = Objects.requireNonNull(uriHandler);
    }

    /**
     * Sets the snapshot on which the change's edit tree is based.
     *
     * @param base the snapshot on which the change is based,
     *  or <code>null</code> if unknown
     */
    public void setBase(ISnapshot base)
    {
        this.base = base;
    }

    /**
     * Returns the snapshot on which the change's edit tree is based,
     * or <code>null</code> if the snapshot is unknown.
     *
     * @return the snapshot on which the change is based,
     *  or <code>null</code> if unknown
     */
    public ISnapshot getBase()
    {
        return base;
    }

    /**
     * Sets the save mode of this change.
     *
     * @param saveMode a save mode (not <code>null</code>)
     */
    public void setSaveMode(SaveMode saveMode)
    {
        this.saveMode = Objects.requireNonNull(saveMode);
    }

    /**
     * Returns the save mode associated with this change.
     *
     * @return the change's save mode (never <code>null</code>)
     */
    public SaveMode getSaveMode()
    {
        return saveMode;
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm)
    {
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
        OperationCanceledException
    {
        RefactoringStatus result = new RefactoringStatus();

        if (base == null)
            return result; // OK

        try (IBuffer buffer = getBuffer(uri, uriHandler))
        {
            if (!base.isEqualTo(buffer.getSnapshot()))
            {
                result.addFatalError(MessageFormat.format(
                    Messages.TextFileChange_Cannot_apply_stale_change,
                    toDisplayString(uri, uriHandler)));
            }
        }
        return result;
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException
    {
        SubMonitor subMonitor = SubMonitor.convert(pm, 1);
        try (IBuffer buffer = getBuffer(uri, uriHandler))
        {
            BufferChangeWithExcludes change = new BufferChangeWithExcludes(
                getEdit());
            List<TextEdit> excludes = new ArrayList<>();
            TextEditBasedChangeGroup[] groups = getChangeGroups();
            for (TextEditBasedChangeGroup group : groups)
            {
                if (!group.isEnabled())
                {
                    excludes.addAll(Arrays.asList(
                        group.getTextEditGroup().getTextEdits()));
                }
            }
            change.setExcludes(excludes);
            change.setBase(base);
            change.setStyle(IBufferChange.CREATE_UNDO
                | IBufferChange.UPDATE_REGIONS);
            change.setSaveMode(saveMode);

            IBufferChange undoChange;

            try
            {
                undoChange = buffer.applyChange(change, subMonitor.split(1,
                    SubMonitor.SUPPRESS_ISCANCELED
                        | SubMonitor.SUPPRESS_BEGINTASK));
            }
            catch (StaleSnapshotException e)
            {
                throw new CoreException(Activator.createErrorStatus(
                    MessageFormat.format(
                        Messages.TextFileChange_Cannot_apply_stale_change,
                        toDisplayString(uri, uriHandler)), e));
            }

            return new UndoTextFileChange(getName(), uri, uriHandler,
                undoChange);
        }
    }

    @Override
    public Object getModifiedElement()
    {
        return uriHandler.getCorrespondingElement(uri);
    }

    @Override
    public Object[] getAffectedObjects()
    {
        Object element = getModifiedElement();
        if (element != null)
            return new Object[] { element };
        return null;
    }

    @Override
    protected IDocument acquireDocument(IProgressMonitor pm)
        throws CoreException
    {
        if (buffer != null)
            throw new AssertionError("The buffer has not been released"); //$NON-NLS-1$
        buffer = getBuffer(uri, uriHandler);
        return buffer.getDocument();
    }

    @Override
    protected void releaseDocument(IDocument document, IProgressMonitor pm)
        throws CoreException
    {
        if (buffer == null)
            throw new AssertionError("The buffer has not been acquired"); //$NON-NLS-1$
        IBuffer b = buffer;
        buffer = null;
        b.release();
    }

    @Override
    protected void commit(IDocument document, IProgressMonitor pm)
        throws CoreException
    {
        throw new AssertionError("This method should not be called"); //$NON-NLS-1$
    }

    @Override
    protected Change createUndoChange(UndoEdit edit)
    {
        throw new AssertionError("This method should not be called"); //$NON-NLS-1$
    }

    /*
     * Buffer change with the ability to selectively exclude single text edits.
     */
    private static class BufferChangeWithExcludes
        extends BufferChange
    {
        private Set<TextEdit> excludes;

        BufferChangeWithExcludes(TextEdit edit)
        {
            super(edit);
        }

        void setExcludes(Collection<TextEdit> excludes)
        {
            if (excludes == null)
                throw new IllegalArgumentException();
            this.excludes = flatten(excludes);
        }

        @Override
        public boolean contains(TextEdit edit)
        {
            if (!super.contains(edit))
                return false;
            if (excludes != null && excludes.contains(edit))
                return false;
            return true;
        }

        private static Set<TextEdit> flatten(Collection<TextEdit> edits)
        {
            Set<TextEdit> result = new HashSet<>();
            for (TextEdit edit : edits)
            {
                flatten(edit, result);
            }
            return result;
        }

        private static void flatten(TextEdit edit, Set<TextEdit> result)
        {
            result.add(edit);
            TextEdit[] children = edit.getChildren();
            for (TextEdit child : children)
            {
                flatten(child, result);
            }
        }
    }
}
