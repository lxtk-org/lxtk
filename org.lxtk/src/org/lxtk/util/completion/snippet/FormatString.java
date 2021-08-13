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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

class FormatString
{
    private final List<Node> nodes;

    static FormatString parse(CharBuffer source, Character stopChar)
    {
        return new Parser(source).parse(stopChar);
    }

    String evaluate(MatchResult matchResult)
    {
        return new Evaluator(matchResult).evaluate(this);
    }

    private FormatString(List<Node> nodes)
    {
        this.nodes = Objects.requireNonNull(nodes);
    }

    private static class Parser
    {
        private final CharBuffer source;

        Parser(CharBuffer source)
        {
            this.source = Objects.requireNonNull(source);
        }

        FormatString parse(Character stopChar)
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
                    Node node = parseGroup();
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
                    if (nextCh == '$' || (stopChar != null && nextCh == stopChar) || nextCh == '\\')
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
            return new FormatString(Collections.unmodifiableList(nodes));
        }

        private Node parseGroup()
        {
            return nextChar('{') ? parseComplexGroup() : parseSimpleGroup();
        }

        private Node parseSimpleGroup()
        {
            Integer group = parseInt();
            if (group != null)
                return new GroupNode(group);
            return null;
        }

        private Node parseComplexGroup()
        {
            Integer group = parseInt();
            if (group != null)
            {
                if (nextChar('}'))
                {
                    return new GroupNode(group);
                }
                else if (nextChar(':'))
                {
                    if (nextChar('/'))
                    {
                        String op = parseIdent(); // intentionally loose
                        if (op != null && nextChar('}'))
                        {
                            return new OpGroupNode(group, op);
                        }
                    }
                    else if (nextChar('+'))
                    {
                        FormatString ifValue = parse('}');
                        if (nextChar('}'))
                        {
                            return new ConditionalGroupNode(group, ifValue, null);
                        }
                    }
                    else if (nextChar('?'))
                    {
                        FormatString ifValue = parse(':');
                        if (nextChar(':'))
                        {
                            FormatString elseValue = parse('}');
                            if (nextChar('}'))
                            {
                                return new ConditionalGroupNode(group, ifValue, elseValue);
                            }
                        }
                    }
                    else
                    {
                        nextChar('-'); // eat '-' if present
                        FormatString elseValue = parse('}');
                        if (nextChar('}'))
                        {
                            return new ConditionalGroupNode(group, null, elseValue);
                        }
                    }
                }
            }
            return null;
        }

        private Integer parseInt()
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
            return Integer.valueOf(sb.toString());
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
    }

    private static class Evaluator
    {
        private final MatchResult matchResult;
        private StringBuilder text;

        Evaluator(MatchResult matchResult)
        {
            this.matchResult = Objects.requireNonNull(matchResult);
        }

        String evaluate(FormatString formatString)
        {
            text = new StringBuilder();
            expand(formatString);
            return Matcher.quoteReplacement(text.toString());
        }

        private void expand(FormatString formatString)
        {
            for (Node node : formatString.nodes)
            {
                if (node instanceof TextNode)
                    expand((TextNode)node);
                else if (node instanceof OpGroupNode)
                    expand((OpGroupNode)node);
                else if (node instanceof ConditionalGroupNode)
                    expand((ConditionalGroupNode)node);
                else if (node instanceof GroupNode)
                    expand((GroupNode)node);
            }
        }

        private void expand(TextNode node)
        {
            text.append(node.text);
        }

        private void expand(GroupNode node)
        {
            String value = getGroupValue(node.group);
            if (value != null)
                text.append(value);
        }

        private void expand(OpGroupNode node)
        {
            String value = getGroupValue(node.group);
            if (value == null || value.isEmpty())
                return;
            switch (node.op)
            {
            case OpGroupNode.UPCASE:
                text.append(value.toUpperCase());
                break;
            case OpGroupNode.DOWNCASE:
                text.append(value.toLowerCase());
                break;
            case OpGroupNode.CAPITALIZE:
                text.append(Character.toUpperCase(value.charAt(0)));
                text.append(value.substring(1));
                break;
            default:
                text.append(value);
            }
        }

        private void expand(ConditionalGroupNode node)
        {
            String value = getGroupValue(node.group);
            if (value != null && node.ifValue != null)
                expand(node.ifValue);
            else if (value == null && node.elseValue != null)
                expand(node.elseValue);
        }

        private String getGroupValue(int group)
        {
            try
            {
                return matchResult.group(group);
            }
            catch (IndexOutOfBoundsException e)
            {
                return null;
            }
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

    private static class GroupNode
        implements Node
    {
        final int group;

        GroupNode(int group)
        {
            if (group < 0)
                throw new IllegalArgumentException();
            this.group = group;
        }
    }

    private static class OpGroupNode
        extends GroupNode
    {
        final static String UPCASE = "upcase"; //$NON-NLS-1$
        final static String DOWNCASE = "downcase"; //$NON-NLS-1$
        final static String CAPITALIZE = "capitalize"; //$NON-NLS-1$

        final String op;

        OpGroupNode(int group, String op)
        {
            super(group);
            this.op = Objects.requireNonNull(op);
        }
    }

    private static class ConditionalGroupNode
        extends GroupNode
    {
        final FormatString ifValue;
        final FormatString elseValue;

        ConditionalGroupNode(int group, FormatString ifValue, FormatString elseValue)
        {
            super(group);
            this.ifValue = ifValue;
            this.elseValue = elseValue;
            if (ifValue == null && elseValue == null)
                throw new NullPointerException();
        }
    }
}
