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
package org.lxtk.lx4e.ui.folding;

import static org.lxtk.lx4e.internal.util.AnnotationUtil.replaceAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.lsp4j.FoldingRange;
import org.lxtk.util.Disposable;

/**
 * Represents folding annotations of a document.
 */
public class FoldingAnnotations
    implements Disposable
{
    private final IDocument document;
    private final ProjectionAnnotationModel model;
    private final Map<FoldingAnnotation, Position> structure = new HashMap<>();
    private boolean disposed;

    /**
     * Constructor.
     *
     * @param document not <code>null</code>
     * @param model not <code>null</code>
     */
    public FoldingAnnotations(IDocument document, ProjectionAnnotationModel model)
    {
        this.document = Objects.requireNonNull(document);
        this.model = Objects.requireNonNull(model);
    }

    /**
     * Returns the associated document.
     *
     * @return the associated document (never <code>null</code>)
     */
    public final IDocument getDocument()
    {
        return document;
    }

    /**
     * Returns the associated annotation model.
     *
     * @return the associated annotation model (never <code>null</code>)
     */
    public final ProjectionAnnotationModel getModel()
    {
        return model;
    }

    /**
     * Updates annotations to reflect the new folding structure.
     *
     * @param foldingRanges may be <code>null</code> or empty
     */
    public final synchronized void update(Collection<FoldingRange> foldingRanges)
    {
        if (disposed)
            return;

        if (foldingRanges == null || foldingRanges.isEmpty())
        {
            clear();
        }
        else
        {
            Collection<FoldingAnnotation> toRemove = new ArrayList<>(structure.keySet());
            Map<FoldingAnnotation, Position> toAdd = new IdentityHashMap<>(foldingRanges.size());

            for (FoldingRange foldingRange : foldingRanges)
            {
                if (foldingRange.getStartLine() == foldingRange.getEndLine())
                    continue;

                Position position;
                try
                {
                    position = createPosition(foldingRange);
                }
                catch (BadLocationException e)
                {
                    // silently ignore: the document might have changed in the meantime
                    continue;
                }

                FoldingAnnotation annotation = createAnnotation(foldingRange);
                if (annotation == null)
                    continue;

                FoldingAnnotation existingAnnotation = findMatch(annotation, position);
                if (existingAnnotation != null)
                    toRemove.remove(existingAnnotation);
                else
                    toAdd.put(annotation, position);
            }

            replaceAnnotations(model, toRemove, toAdd);
            structure.keySet().removeAll(toRemove);
            structure.putAll(toAdd);
        }
    }

    /**
     * Removes all annotations currently managed by this object.
     */
    public final synchronized void clear()
    {
        replaceAnnotations(model, structure.keySet(), null);
        structure.clear();
    }

    @Override
    public final synchronized void dispose()
    {
        clear();
        disposed = true;
    }

    /**
     * Returns whether there is a match between the given annotations.
     *
     * @param a1 never <code>null</code>
     * @param a2 never <code>null</code>
     * @return <code>true</code> if the given annotations match,
     *  and <code>false</code> otherwise
     */
    protected boolean isMatch(FoldingAnnotation a1, FoldingAnnotation a2)
    {
        return Objects.equals(a1.getKind(), a2.getKind());
    }

    /**
     * Creates and returns a {@link FoldingAnnotation} representing
     * the given folding range.
     *
     * @param foldingRange never <code>null</code>
     * @return the created annotation, or <code>null</code> if none
     */
    protected FoldingAnnotation createAnnotation(FoldingRange foldingRange)
    {
        FoldingAnnotation annotation = new FoldingAnnotation();
        annotation.setKind(foldingRange.getKind());
        return annotation;
    }

    /**
     * Creates and returns a {@link Position} representing the given folding range.
     *
     * @param foldingRange never <code>null</code>
     * @return the created position (not <code>null</code>)
     * @throws BadLocationException
     */
    protected Position createPosition(FoldingRange foldingRange) throws BadLocationException
    {
        IRegion region = getRegion(foldingRange);
        return new Position(region.getOffset(), region.getLength());
    }

    /**
     * Returns a corresponding {@link Region} for the given folding range.
     *
     * @param foldingRange never <code>null</code>
     * @return the corresponding region (not <code>null</code>)
     * @throws BadLocationException
     */
    protected IRegion getRegion(FoldingRange foldingRange) throws BadLocationException
    {
        int offset = document.getLineOffset(foldingRange.getStartLine());
        int endLine = foldingRange.getEndLine();
        int endOffset =
            document.getNumberOfLines() > endLine + 1 ? document.getLineOffset(endLine + 1)
                : document.getLineOffset(endLine) + document.getLineLength(endLine);
        return new Region(offset, endOffset - offset);
    }

    private FoldingAnnotation findMatch(FoldingAnnotation annotation, Position position)
    {
        for (Entry<FoldingAnnotation, Position> entry : structure.entrySet())
        {
            FoldingAnnotation existingAnnotation = entry.getKey();
            Position existingPosition = entry.getValue();

            if (!existingAnnotation.isMarkedDeleted() && !existingPosition.isDeleted()
                && position.equals(existingPosition) && isMatch(annotation, existingAnnotation))
                return existingAnnotation;
        }
        return null;
    }
}
