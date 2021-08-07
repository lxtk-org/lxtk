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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Note: variable transformations are yet to be supported
class SnippetParser
{
    private final String source;
    private final int sourceLength;
    private final SnippetContext context;
    private int cursor;

    SnippetParser(String source, SnippetContext context)
    {
        this.source = Objects.requireNonNull(source);
        this.sourceLength = source.length();
        this.context = Objects.requireNonNull(context);
    }

    Snippet parse() throws SnippetException
    {
        cursor = 0;
        SnippetDescription rootDesc = parseAny();
        try
        {
            return new Evaluator(rootDesc).evaluate();
        }
        catch (SnippetException e)
        {
            throw new SnippetException(
                MessageFormat.format(Messages.getString("SnippetParser.EvaluationError"), source), //$NON-NLS-1$
                e);
        }
    }

    private SnippetDescription parseAny()
    {
        SnippetDescriptionBuilder builder = new SnippetDescriptionBuilder();
        boolean nested = cursor > 0;
        while (hasNextChar())
        {
            char ch = peekNextChar();
            if (ch == '}' && nested)
                break;
            if (ch == '$')
            {
                int savedCursor = cursor;
                nextChar(); // eat '$'
                if (parseTabStopOrVariable(builder))
                    continue;
                cursor = savedCursor; // backtrack
            }
            nextChar();
            if (ch == '\\' && hasNextChar())
            {
                char nextCh = peekNextChar();
                if (nextCh == '$' || nextCh == '}' || nextCh == '\\')
                {
                    builder.text.append(nextCh);
                    nextChar();
                    continue;
                }
            }
            builder.text.append(ch);
        }
        return builder.toSnippetDescription();
    }

    private boolean parseTabStopOrVariable(SnippetDescriptionBuilder builder)
    {
        return nextChar('{') ? parseComplexTabStopOrVariable(builder)
            : parseSimpleTabStopOrVariable(builder);
    }

    private boolean parseSimpleTabStopOrVariable(SnippetDescriptionBuilder builder)
    {
        String number = parseInt();
        if (number != null)
        {
            builder.tabStops.add(new TabStopDescription(number, builder.text.length()));
            return true;
        }
        String name = parseIdent();
        if (name != null)
        {
            if (context.isKnownVariable(name))
                builder.text.append(context.resolveVariable(name, "")); //$NON-NLS-1$
            else
                builder.tabStops.add(new PlaceholderDescription(name, builder.text.length(),
                    new SnippetDescription(name, Collections.emptyList())));
            return true;
        }
        return false;
    }

    private boolean parseComplexTabStopOrVariable(SnippetDescriptionBuilder builder)
    {
        String number = parseInt();
        if (number != null)
        {
            if (nextChar('}'))
            {
                builder.tabStops.add(new TabStopDescription(number, builder.text.length()));
                return true;
            }
            else if (nextChar(':'))
            {
                SnippetDescription defaultValue = parseAny();
                if (nextChar('}'))
                {
                    builder.tabStops.add(
                        new PlaceholderDescription(number, builder.text.length(), defaultValue));
                    return true;
                }
            }
            else if (nextChar('|'))
            {
                List<String> values = new ArrayList<>();
                do
                {
                    values.add(parseChoiceValue());
                }
                while (nextChar(','));

                if (nextChar('|') && nextChar('}'))
                {
                    builder.tabStops.add(new ChoiceDescription(number, builder.text.length(),
                        Collections.unmodifiableList(values)));
                    return true;
                }
            }
        }
        else
        {
            String name = parseIdent();
            if (name != null)
            {
                if (nextChar('}'))
                {
                    if (context.isKnownVariable(name))
                        builder.text.append(context.resolveVariable(name, "")); //$NON-NLS-1$
                    else
                        builder.tabStops.add(new PlaceholderDescription(name, builder.text.length(),
                            new SnippetDescription(name, Collections.emptyList())));
                    return true;
                }
                else if (nextChar(':'))
                {
                    SnippetDescription defaultValue = parseAny();
                    if (nextChar('}'))
                    {
                        String value = context.resolveVariable(name);
                        if (value != null)
                            builder.text.append(value);
                        else
                        {
                            for (TabStopDescription tabStop : defaultValue.tabStops)
                            {
                                builder.tabStops.add(tabStop.adjust(builder.text.length()));
                            }
                            builder.text.append(defaultValue.text);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String parseChoiceValue()
    {
        StringBuilder value = new StringBuilder();
        while (hasNextChar())
        {
            char ch = peekNextChar();
            if (ch == ',' || ch == '|')
                break;
            nextChar();
            if (ch == '\\' && hasNextChar())
            {
                char nextCh = peekNextChar();
                if (nextCh == ',' || nextCh == '|' || nextCh == '\\')
                {
                    value.append(nextCh);
                    nextChar();
                    continue;
                }
            }
            value.append(ch);
        }
        return value.toString();
    }

    private String parseInt()
    {
        StringBuilder sb = null;
        while (hasNextChar() && isDigit(peekNextChar()))
        {
            if (sb == null)
                sb = new StringBuilder();
            sb.append(nextChar());
        }
        if (sb == null)
            return null;
        return removeLeadingZeroes(sb);
    }

    private static String removeLeadingZeroes(StringBuilder sb)
    {
        int index = 0, length = sb.length();
        while (index < length - 1 && sb.charAt(index) == '0')
            ++index;
        return sb.substring(index);
    }

    private String parseIdent()
    {
        if (!(hasNextChar() && isIdentStart(peekNextChar())))
            return null;
        StringBuilder sb = new StringBuilder();
        sb.append(nextChar());
        while (hasNextChar() && isIdentPart(peekNextChar()))
            sb.append(nextChar());
        return sb.toString();
    }

    private static boolean isDigit(char ch)
    {
        return ch >= '0' && ch <= '9';
    }

    private static boolean isIdentStart(char ch)
    {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
    }

    private static boolean isIdentPart(char ch)
    {
        return isIdentStart(ch) || isDigit(ch);
    }

    private boolean hasNextChar()
    {
        return cursor < sourceLength;
    }

    private char peekNextChar()
    {
        return source.charAt(cursor);
    }

    private char nextChar()
    {
        return source.charAt(cursor++);
    }

    private boolean nextChar(char ch)
    {
        if (hasNextChar() && peekNextChar() == ch)
        {
            nextChar();
            return true;
        }
        return false;
    }

    private static class SnippetDescriptionBuilder
    {
        final StringBuilder text = new StringBuilder();
        final List<TabStopDescription> tabStops = new ArrayList<>();

        SnippetDescription toSnippetDescription()
        {
            return new SnippetDescription(text.toString(), Collections.unmodifiableList(tabStops));
        }
    }

    private static class SnippetDescription
    {
        final String text;
        final List<TabStopDescription> tabStops;

        SnippetDescription(String text, List<TabStopDescription> tabStops)
        {
            this.text = Objects.requireNonNull(text);
            this.tabStops = Objects.requireNonNull(tabStops);
        }
    }

    private static class TabStopDescription
    {
        final String id;
        final int offset;

        TabStopDescription(String id, int offset)
        {
            this.id = Objects.requireNonNull(id);
            this.offset = offset;
        }

        TabStopDescription adjust(int offsetDelta)
        {
            return new TabStopDescription(id, offset + offsetDelta);
        }
    }

    private static class PlaceholderDescription
        extends TabStopDescription
    {
        final SnippetDescription defaultValue;

        PlaceholderDescription(String id, int offset, SnippetDescription defaultValue)
        {
            super(id, offset);
            this.defaultValue = Objects.requireNonNull(defaultValue);
        }

        @Override
        PlaceholderDescription adjust(int offsetDelta)
        {
            return new PlaceholderDescription(id, offset + offsetDelta, defaultValue);
        }
    }

    private static class ChoiceDescription
        extends TabStopDescription
    {
        final List<String> values;

        ChoiceDescription(String id, int offset, List<String> values)
        {
            super(id, offset);
            this.values = Objects.requireNonNull(values);
        }

        @Override
        ChoiceDescription adjust(int offsetDelta)
        {
            return new ChoiceDescription(id, offset + offsetDelta, values);
        }
    }

    private static class Evaluator
    {
        private final SnippetDescription rootDesc;
        private final SnippetBuilder snippetBuilder = new SnippetBuilder();
        private final Map<String, TabStopDescription> placeholdersOrChoices = new HashMap<>();

        Evaluator(SnippetDescription rootDesc)
        {
            this.rootDesc = Objects.requireNonNull(rootDesc);
        }

        Snippet evaluate() throws SnippetException
        {
            expand(rootDesc, null);
            return snippetBuilder.toSnippet();
        }

        private void expand(SnippetDescription desc, TraversalState state) throws SnippetException
        {
            int offset = 0;
            for (TabStopDescription tabStop : desc.tabStops)
            {
                snippetBuilder.text.append(desc.text.substring(offset, tabStop.offset));
                offset = tabStop.offset;

                expand(tabStop, state);
            }
            snippetBuilder.text.append(desc.text.substring(offset));
        }

        private void expand(TabStopDescription tabStop, TraversalState state)
            throws SnippetException
        {
            if (state == null)
                state = new TraversalState();

            TabStopBuilder tabStopBuilder = snippetBuilder.getTabStopBuilder(tabStop.id);
            TabStopDescription placeholderOrChoice = getPlaceholderOrChoice(tabStop.id);
            state.mirror |= tabStop != placeholderOrChoice;
            int length = snippetBuilder.text.length();

            if (!state.mirror)
                tabStopBuilder.offsets.add(0, length);
            else
                tabStopBuilder.offsets.add(length);

            if (placeholderOrChoice instanceof PlaceholderDescription)
            {
                PlaceholderDescription placeholder = (PlaceholderDescription)placeholderOrChoice;
                if (!state.visited.add(placeholder))
                    throw new SnippetException(MessageFormat.format(
                        Messages.getString("SnippetParser.EvaluationError.CyclicReference"), //$NON-NLS-1$
                        placeholder.id));
                expand(placeholder.defaultValue, state);
                if (tabStopBuilder.values.isEmpty())
                    tabStopBuilder.values.add(snippetBuilder.text.substring(length));
            }
            else if (placeholderOrChoice instanceof ChoiceDescription)
            {
                ChoiceDescription choice = (ChoiceDescription)placeholderOrChoice;
                snippetBuilder.text.append(choice.values.get(0));
                if (tabStopBuilder.values.isEmpty())
                    tabStopBuilder.values.addAll(choice.values);
            }
        }

        private TabStopDescription getPlaceholderOrChoice(String id)
        {
            return placeholdersOrChoices.computeIfAbsent(id,
                x -> findPlaceholderOrChoice(id, rootDesc));
        }

        private static TabStopDescription findPlaceholderOrChoice(String id,
            SnippetDescription desc)
        {
            for (TabStopDescription tabStop : desc.tabStops)
            {
                if (tabStop.id.equals(id) && (tabStop instanceof PlaceholderDescription
                    || tabStop instanceof ChoiceDescription))
                    return tabStop;

                if (tabStop instanceof PlaceholderDescription)
                {
                    TabStopDescription result =
                        findPlaceholderOrChoice(id, ((PlaceholderDescription)tabStop).defaultValue);
                    if (result != null)
                        return result;
                }
            }
            return null;
        }

        private static class SnippetBuilder
        {
            final StringBuilder text = new StringBuilder();
            final Map<String, TabStopBuilder> tabStopBuilders = new HashMap<>();

            TabStopBuilder getTabStopBuilder(String id)
            {
                return tabStopBuilders.computeIfAbsent(id, TabStopBuilder::new);
            }

            Snippet toSnippet()
            {
                TabStop[] tabStops = new TabStop[tabStopBuilders.size()];
                int i = 0;
                for (TabStopBuilder tabStopBuilder : tabStopBuilders.values())
                {
                    tabStops[i++] = tabStopBuilder.toTabStop();
                }
                Arrays.sort(tabStops, TabStop::compareTo);
                return new Snippet(text.toString(), tabStops);
            }
        }

        private static class TabStopBuilder
        {
            private final static String[] NO_VALUES = new String[0];

            final String id;
            final List<Integer> offsets = new ArrayList<>();
            final List<String> values = new ArrayList<>();

            TabStopBuilder(String id)
            {
                this.id = Objects.requireNonNull(id);
            }

            TabStop toTabStop()
            {
                int[] offsetsArray = new int[offsets.size()];
                Arrays.setAll(offsetsArray, offsets::get);
                return new TabStop(id, offsetsArray, values.toArray(NO_VALUES));
            }
        }

        private static class TraversalState
        {
            boolean mirror;
            Set<PlaceholderDescription> visited = new HashSet<>();
        }
    }
}
