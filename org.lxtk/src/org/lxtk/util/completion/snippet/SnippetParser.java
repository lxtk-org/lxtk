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

import java.nio.CharBuffer;
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
import java.util.regex.PatternSyntaxException;

class SnippetParser
{
    private final CharBuffer source;
    private final SnippetContext context;

    SnippetParser(CharBuffer source, SnippetContext context)
    {
        this.source = Objects.requireNonNull(source);
        this.context = Objects.requireNonNull(context);
    }

    Snippet parse(Character stopChar) throws SnippetException
    {
        SnippetDescription rootDesc = parseAny(stopChar);
        return new Evaluator(rootDesc, context).evaluate();
    }

    private SnippetDescription parseAny(Character stopChar)
    {
        List<Node> nodes = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        while (hasNextChar())
        {
            char ch = peekNextChar();
            if (stopChar != null && ch == stopChar)
                break;
            if (ch == '$')
            {
                int position = source.position();
                nextChar(); // eat '$'
                Node node = parseTabStopOrVariable();
                if (node != null)
                {
                    if (text.length() > 0)
                    {
                        nodes.add(new TextNode(text.toString()));
                        text = new StringBuilder();
                    }
                    nodes.add(node);
                    continue;
                }
                source.position(position); // backtrack
            }
            nextChar();
            if (ch == '\\' && hasNextChar())
            {
                char nextCh = peekNextChar();
                if (nextCh == '$' || nextCh == '}' || nextCh == '\\')
                {
                    text.append(nextCh);
                    nextChar();
                    continue;
                }
            }
            text.append(ch);
        }
        if (text.length() > 0)
            nodes.add(new TextNode(text.toString()));
        return new SnippetDescription(Collections.unmodifiableList(nodes));
    }

    private Node parseTabStopOrVariable()
    {
        return nextChar('{') ? parseComplexTabStopOrVariable() : parseSimpleTabStopOrVariable();
    }

    private Node parseSimpleTabStopOrVariable()
    {
        String number = parseInt();
        if (number != null)
        {
            return new TabStopNode(number);
        }
        String name = parseIdent();
        if (name != null)
        {
            return context.isKnownVariable(name) ? new VariableNode(name) : new PlaceholderNode(
                name, new SnippetDescription(Collections.singletonList(new TextNode(name))));
        }
        return null;
    }

    private Node parseComplexTabStopOrVariable()
    {
        String number = parseInt();
        if (number != null)
        {
            if (nextChar('}'))
            {
                return new TabStopNode(number);
            }
            else if (nextChar(':'))
            {
                SnippetDescription defaultValue = parseAny('}');
                if (nextChar('}'))
                {
                    return new PlaceholderNode(number, defaultValue);
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
                    return new ChoiceNode(number, Collections.unmodifiableList(values));
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
                    return context.isKnownVariable(name) ? new VariableNode(name)
                        : new PlaceholderNode(name,
                            new SnippetDescription(Collections.singletonList(new TextNode(name))));
                }
                else if (nextChar(':'))
                {
                    SnippetDescription defaultValue = parseAny('}');
                    if (nextChar('}'))
                    {
                        return new VariableNodeWithDefault(name, defaultValue);
                    }
                }
                else if (nextChar('/'))
                {
                    String regex = parseRegex();
                    if (nextChar('/'))
                    {
                        FormatString format = parseFormatString();
                        if (nextChar('/'))
                        {
                            String flags = parseRegexFlags();
                            if (nextChar('}'))
                            {
                                return new VariableTransformNode(name, regex, format, flags);
                            }
                        }
                    }
                }
            }
        }
        return null;
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

    private String parseRegex()
    {
        StringBuilder text = new StringBuilder();
        while (hasNextChar())
        {
            char ch = peekNextChar();
            if (ch == '/')
                break;
            nextChar();
            if (ch == '\\' && hasNextChar())
            {
                char nextCh = peekNextChar();
                if (nextCh == '/' || nextCh == '\\')
                {
                    text.append(nextCh);
                    nextChar();
                    continue;
                }
            }
            text.append(ch);
        }
        return text.toString();
    }

    private FormatString parseFormatString()
    {
        return FormatString.parse(source, '/');
    }

    private String parseRegexFlags()
    {
        StringBuilder sb = new StringBuilder();
        while (hasNextChar() && isRegexFlag(peekNextChar()))
            sb.append(nextChar());
        return sb.toString();
    }

    private static boolean isRegexFlag(char ch)
    {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'); // intentionally loose
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
        return source.position() < source.limit();
    }

    private char peekNextChar()
    {
        return source.get(source.position());
    }

    private char nextChar()
    {
        return source.get();
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

    private static class SnippetDescription
    {
        final List<Node> nodes;

        SnippetDescription(List<Node> nodes)
        {
            this.nodes = Objects.requireNonNull(nodes);
        }
    }

    private interface Node
    {
    }

    private static class TextNode
        implements Node
    {
        final String text;

        TextNode(String text)
        {
            this.text = Objects.requireNonNull(text);
        }
    }

    private static class TabStopNode
        implements Node
    {
        final String id;

        TabStopNode(String id)
        {
            this.id = Objects.requireNonNull(id);
        }
    }

    private static class PlaceholderNode
        extends TabStopNode
    {
        final SnippetDescription defaultValue;

        PlaceholderNode(String id, SnippetDescription defaultValue)
        {
            super(id);
            this.defaultValue = Objects.requireNonNull(defaultValue);
        }
    }

    private static class ChoiceNode
        extends TabStopNode
    {
        final List<String> values;

        ChoiceNode(String id, List<String> values)
        {
            super(id);
            this.values = Objects.requireNonNull(values);
        }
    }

    private static class VariableNode
        implements Node
    {
        final String name;

        VariableNode(String name)
        {
            this.name = Objects.requireNonNull(name);
        }
    }

    private static class VariableNodeWithDefault
        extends VariableNode
    {
        final SnippetDescription defaultValue;

        VariableNodeWithDefault(String name, SnippetDescription defaultValue)
        {
            super(name);
            this.defaultValue = Objects.requireNonNull(defaultValue);
        }
    }

    private static class VariableTransformNode
        extends VariableNode
    {
        final String regex, flags;
        final FormatString format;

        VariableTransformNode(String name, String regex, FormatString format, String flags)
        {
            super(name);
            this.regex = Objects.requireNonNull(regex);
            this.format = Objects.requireNonNull(format);
            this.flags = Objects.requireNonNull(flags);
        }
    }

    private static class Evaluator
    {
        private final SnippetDescription rootDesc;
        private final SnippetContext context;
        private final SnippetBuilder snippetBuilder = new SnippetBuilder();
        private final Map<String, TabStopNode> placeholdersOrChoices = new HashMap<>();

        Evaluator(SnippetDescription rootDesc, SnippetContext context)
        {
            this.rootDesc = Objects.requireNonNull(rootDesc);
            this.context = Objects.requireNonNull(context);
        }

        Snippet evaluate() throws SnippetException
        {
            expand(rootDesc, null);
            return snippetBuilder.toSnippet();
        }

        private void expand(SnippetDescription desc, TraversalState state) throws SnippetException
        {
            for (Node node : desc.nodes)
            {
                if (node instanceof TextNode)
                    expand((TextNode)node);
                else if (node instanceof TabStopNode)
                    expand((TabStopNode)node, state);
                else if (node instanceof VariableNode)
                    expand((VariableNode)node, state);
            }
        }

        private void expand(TextNode node)
        {
            snippetBuilder.text.append(node.text);
        }

        private void expand(TabStopNode tabStop, TraversalState state) throws SnippetException
        {
            if (state == null)
                state = new TraversalState();

            TabStopBuilder tabStopBuilder = snippetBuilder.getTabStopBuilder(tabStop.id);
            TabStopNode placeholderOrChoice = getPlaceholderOrChoice(tabStop.id);
            state.mirror |= tabStop != placeholderOrChoice;
            int length = snippetBuilder.text.length();

            if (!state.mirror)
                tabStopBuilder.offsets.add(0, length);
            else
                tabStopBuilder.offsets.add(length);

            if (placeholderOrChoice instanceof PlaceholderNode)
            {
                PlaceholderNode placeholder = (PlaceholderNode)placeholderOrChoice;
                if (!state.visited.add(placeholder))
                    throw new SnippetException(MessageFormat.format(
                        Messages.getString("SnippetParser.EvaluationError.CyclicReference"), //$NON-NLS-1$
                        placeholder.id));
                expand(placeholder.defaultValue, state);
                if (tabStopBuilder.values.isEmpty())
                    tabStopBuilder.values.add(snippetBuilder.text.substring(length));
            }
            else if (placeholderOrChoice instanceof ChoiceNode)
            {
                ChoiceNode choice = (ChoiceNode)placeholderOrChoice;
                snippetBuilder.text.append(choice.values.get(0));
                if (tabStopBuilder.values.isEmpty())
                    tabStopBuilder.values.addAll(choice.values);
            }
        }

        private void expand(VariableNode var, TraversalState state) throws SnippetException
        {
            String value = context.resolveVariable(var.name);
            if (value == null && var instanceof VariableNodeWithDefault)
            {
                expand(((VariableNodeWithDefault)var).defaultValue, state);
            }
            else
            {
                if (value == null)
                    value = ""; //$NON-NLS-1$
                if (var instanceof VariableTransformNode)
                {
                    VariableTransformNode vt = (VariableTransformNode)var;
                    Transformer transformer;
                    try
                    {
                        transformer = Transformer.compile(vt.regex, vt.format, vt.flags);
                    }
                    catch (PatternSyntaxException e)
                    {
                        throw new SnippetException(e);
                    }
                    value = transformer.transform(value);
                }
                snippetBuilder.text.append(value);
            }
        }

        private TabStopNode getPlaceholderOrChoice(String id)
        {
            return placeholdersOrChoices.computeIfAbsent(id,
                x -> findPlaceholderOrChoice(id, rootDesc));
        }

        private static TabStopNode findPlaceholderOrChoice(String id, SnippetDescription desc)
        {
            for (Node node : desc.nodes)
            {
                if ((node instanceof PlaceholderNode || node instanceof ChoiceNode)
                    && ((TabStopNode)node).id.equals(id))
                    return (TabStopNode)node;

                SnippetDescription nestedDesc = null;
                if (node instanceof PlaceholderNode)
                    nestedDesc = ((PlaceholderNode)node).defaultValue;
                else if (node instanceof VariableNodeWithDefault)
                    nestedDesc = ((VariableNodeWithDefault)node).defaultValue;

                if (nestedDesc != null)
                {
                    TabStopNode result = findPlaceholderOrChoice(id, nestedDesc);
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
            Set<PlaceholderNode> visited = new HashSet<>();
        }
    }
}
