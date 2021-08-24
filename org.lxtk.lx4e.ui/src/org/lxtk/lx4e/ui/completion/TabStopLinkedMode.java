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
package org.lxtk.lx4e.ui.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.InclusivePositionUpdater;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.lxtk.lx4e.internal.ui.Activator;
import org.lxtk.lx4e.internal.ui.LinkedEditingPubSub;
import org.lxtk.util.completion.snippet.TabStop;

class TabStopLinkedMode
{
    private final ITextViewer viewer;
    private final IDocument document;
    private final TabStops tabStops;
    private final ExitPosition exitPosition;
    private final ILinkedModeListener listener = new ILinkedModeListener()
    {
        @Override
        public void left(LinkedModeModel model, int flags)
        {
            if (flags == ILinkedModeListener.UPDATE_CARET)
                next();
            else if (flags == ILinkedModeListener.SELECT)
                previous();
            else
                leave();
        }

        @Override
        public void suspend(LinkedModeModel model)
        {
        }

        @Override
        public void resume(LinkedModeModel model, int flags)
        {
        }
    };
    private final TabStopAnnotationModel annotationModel = new TabStopAnnotationModel();
    private LinkedModeUI linkedModeUI;

    static TabStopLinkedMode start(ITextViewer viewer, int start, int exitOffset,
        List<TabStop> tabStops) throws BadLocationException
    {
        return start(viewer, TabStops.of(tabStops, start), new ExitPosition(start + exitOffset));
    }

    private static TabStopLinkedMode start(ITextViewer viewer, TabStops tabStops,
        ExitPosition exitPosition) throws BadLocationException
    {
        TabStopLinkedMode linkedMode = new TabStopLinkedMode(viewer, tabStops, exitPosition);
        linkedMode.connect();
        LinkedEditingPubSub.INSTANCE.fireLinkedEditingStarted(viewer);
        try
        {
            linkedMode.enter(tabStops.current());
        }
        catch (Throwable e)
        {
            linkedMode.leave();
            throw e;
        }
        return linkedMode;
    }

    private TabStopLinkedMode(ITextViewer viewer, TabStops tabStops, ExitPosition exitPosition)
    {
        this.viewer = Objects.requireNonNull(viewer);
        this.document = Objects.requireNonNull(viewer.getDocument());
        this.tabStops = Objects.requireNonNull(tabStops);
        this.exitPosition = Objects.requireNonNull(exitPosition);
    }

    IRegion getSelectedRegion()
    {
        if (linkedModeUI != null)
            return linkedModeUI.getSelectedRegion();
        return null;
    }

    private void next()
    {
        TabStopInfo tabStop = tabStops.next();
        if (tabStop == null)
        {
            leave();
        }
        else
        {
            try
            {
                enter(tabStop);
            }
            catch (BadLocationException e)
            {
                Activator.logError(e);
                leave();
            }
        }
    }

    private void previous()
    {
        TabStopInfo tabStop = tabStops.previous();
        if (tabStop == null) // if there is no previous tab stop...
        {
            // ...then re-entering the current one seems to be a better choice for UX than leaving
            tabStop = tabStops.current();
        }
        try
        {
            enter(tabStop);
        }
        catch (BadLocationException e)
        {
            Activator.logError(e);
            leave();
        }
    }

    private void enter(TabStopInfo tabStop) throws BadLocationException
    {
        if (viewer.getDocument() != document)
        {
            leave();
            return;
        }

        LinkedModeModel linkedModeModel = new LinkedModeModel();
        linkedModeModel.addGroup(tabStop.toLinkedPositionGroup(document));
        linkedModeModel.forceInstall();
        linkedModeModel.addLinkingListener(listener);

        linkedModeUI = new LinkedModeUI(linkedModeModel, viewer);
        // Note that we rely on the exit position and the cycling mode for the linkedModeUI
        // having been set with NO_STOP and CYCLE_NEVER flags respectively. With these settings,
        // whenever LinkedModeUI#next()/previous() are called, the linkedModeUI will leave with
        // UPDATE_CARET/SELECT flags respectively, which we handle in our ILinkedModeListener
        // (see #listener).
        linkedModeUI.setExitPosition(viewer, exitPosition.getOffset(), 0,
            LinkedPositionGroup.NO_STOP);
        linkedModeUI.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
        linkedModeUI.enter();
    }

    private void connect() throws BadLocationException
    {
        try
        {
            tabStops.track(document);
            exitPosition.track(document);
        }
        catch (BadLocationException e)
        {
            disconnect(); // make sure nothing stays connected
            throw e;
        }
        annotationModel.connect(viewer);
        annotationModel.setPositions(tabStops.getPositions());
    }

    private void disconnect()
    {
        tabStops.untrack(document);
        exitPosition.untrack(document);
        annotationModel.removeAllAnnotations();
        annotationModel.disconnect(viewer);
    }

    private void leave()
    {
        LinkedEditingPubSub.INSTANCE.fireLinkedEditingStopped(viewer);
        disconnect();
    }

    private static class ExitPosition
    {
        private final Position position;
        private final String category = toString();
        private final IPositionUpdater updater = new DefaultPositionUpdater(category);

        ExitPosition(int offset)
        {
            position = new Position(offset);
        }

        int getOffset()
        {
            return position.getOffset();
        }

        void track(IDocument document) throws BadLocationException
        {
            document.addPositionCategory(category);
            document.addPositionUpdater(updater);
            try
            {
                document.addPosition(category, position);
            }
            catch (BadPositionCategoryException e)
            {
                throw new AssertionError(e);
            }
        }

        void untrack(IDocument document)
        {
            document.removePositionUpdater(updater);
            if (document.containsPositionCategory(category))
            {
                try
                {
                    document.removePositionCategory(category); // also removes the category's positions
                }
                catch (BadPositionCategoryException e)
                {
                    throw new AssertionError(e);
                }
            }
        }
    }

    private static class TabStops
    {
        private final TabStopInfo[] tabStops;
        private final String category = toString();
        private final IPositionUpdater updater = new InclusivePositionUpdater(category);
        private int current;

        static TabStops of(List<TabStop> tabStops, int start)
        {
            TabStopInfo[] arr = new TabStopInfo[tabStops.size()];

            int i = 0;
            for (TabStop tabStop : tabStops)
                arr[i++] = TabStopInfo.of(tabStop, start);

            return new TabStops(arr);
        }

        private TabStops(TabStopInfo[] tabStops)
        {
            if (tabStops.length == 0)
                throw new IllegalArgumentException();
            this.tabStops = tabStops;
        }

        TabStopInfo current()
        {
            return tabStops[current];
        }

        TabStopInfo next()
        {
            Integer index = getNextIndex();
            if (index == null)
                return null;
            current = index;
            return current();
        }

        private Integer getNextIndex()
        {
            int index = current;
            while (index < tabStops.length - 1)
            {
                TabStopInfo tabStop = tabStops[++index];
                if (!tabStop.isEmpty())
                    return index;
            }
            return null;
        }

        TabStopInfo previous()
        {
            Integer index = getPreviousIndex();
            if (index == null)
                return null;
            current = index;
            return current();
        }

        private Integer getPreviousIndex()
        {
            int index = current;
            while (index > 0)
            {
                TabStopInfo tabStop = tabStops[--index];
                if (!tabStop.isEmpty())
                    return index;
            }
            return null;
        }

        List<Position> getPositions()
        {
            List<Position> result = new ArrayList<>();
            for (TabStopInfo tabStop : tabStops)
            {
                result.addAll(tabStop.getPositions());
            }
            return result;
        }

        void track(IDocument document) throws BadLocationException
        {
            document.addPositionCategory(category);
            document.addPositionUpdater(updater);
            for (TabStopInfo tabStop : tabStops)
            {
                try
                {
                    tabStop.track(document, category);
                }
                catch (BadPositionCategoryException e)
                {
                    throw new AssertionError(e);
                }
            }
        }

        void untrack(IDocument document)
        {
            document.removePositionUpdater(updater);
            if (document.containsPositionCategory(category))
            {
                try
                {
                    document.removePositionCategory(category); // also removes the category's positions
                }
                catch (BadPositionCategoryException e)
                {
                    throw new AssertionError(e);
                }
            }
        }
    }

    private static class TabStopInfo
    {
        private final Position[] positions;
        private final String[] values;

        static TabStopInfo of(TabStop tabStop, int start)
        {
            int[] offsets = tabStop.getOffsets();
            int offsetsLength = offsets.length;

            String[] values = tabStop.getValues();
            int valuesLength = values.length;

            int length = valuesLength > 0 ? values[0].length() : 0;

            Position[] positions = new Position[offsetsLength];
            for (int i = 0; i < offsetsLength; i++)
                positions[i] = new Position(start + offsets[i], length);

            return new TabStopInfo(positions, values);
        }

        private TabStopInfo(Position[] positions, String[] values)
        {
            this.positions = Objects.requireNonNull(positions);
            this.values = Objects.requireNonNull(values);
        }

        boolean isEmpty()
        {
            for (Position p : positions)
            {
                if (!p.isDeleted())
                    return false;
            }
            return true;
        }

        List<Position> getPositions()
        {
            List<Position> result = new ArrayList<>();
            for (Position p : positions)
            {
                if (!p.isDeleted())
                    result.add(p);
            }
            return result;
        }

        LinkedPositionGroup toLinkedPositionGroup(IDocument document) throws BadLocationException
        {
            LinkedPositionGroup group = new LinkedPositionGroup();
            for (Position p : positions)
            {
                if (!p.isDeleted())
                {
                    group.addPosition(group.isEmpty() && values.length > 1
                        ? new LazyProposalPosition(document, p.getOffset(), p.getLength(), values)
                        : new LinkedPosition(document, p.getOffset(), p.getLength()));
                }
            }
            return group;
        }

        void track(IDocument document, String category)
            throws BadLocationException, BadPositionCategoryException
        {
            for (Position p : positions)
                document.addPosition(category, p);
        }

        private static class LazyProposalPosition
            extends ProposalPosition
        {
            private final String[] values;

            LazyProposalPosition(IDocument document, int offset, int length, String[] values)
            {
                super(document, offset, length, null);
                this.values = Objects.requireNonNull(values);
            }

            @Override
            public ICompletionProposal[] getChoices()
            {
                int length = values.length;
                ICompletionProposal[] result = new ICompletionProposal[length];
                for (int i = 0; i < length; i++)
                {
                    result[i] = new ChoiceProposal(values[i]);
                }
                return result;
            }

            private class ChoiceProposal
                implements ICompletionProposal
            {
                private final String value;

                ChoiceProposal(String value)
                {
                    this.value = Objects.requireNonNull(value);
                }

                @Override
                public String getDisplayString()
                {
                    return value;
                }

                @Override
                public void apply(IDocument document)
                {
                    try
                    {
                        replace(document, getOffset(), getLength(), value);
                    }
                    catch (BadLocationException e)
                    {
                        Activator.logError(e); // should never happen
                    }
                }

                private void replace(IDocument document, int offset, int length, String string)
                    throws BadLocationException
                {
                    if (!document.get(offset, length).equals(string))
                        document.replace(offset, length, string);
                }

                @Override
                public Point getSelection(IDocument document)
                {
                    return null;
                }

                @Override
                public String getAdditionalProposalInfo()
                {
                    return null;
                }

                @Override
                public Image getImage()
                {
                    return null;
                }

                @Override
                public IContextInformation getContextInformation()
                {
                    return null;
                }
            }
        }
    }
}
