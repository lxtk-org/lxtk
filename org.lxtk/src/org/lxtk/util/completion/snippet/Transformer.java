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

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class Transformer
{
    private final Pattern pattern;
    private final Function<MatchResult, String> replacer;
    private final boolean global;

    static Transformer compile(String regex, FormatString format, String flags)
        throws PatternSyntaxException
    {
        Pattern pattern = Pattern.compile(regex, toIntFlags(flags));
        Function<MatchResult, String> replacer = format::evaluate;
        boolean global = contains(flags, 'g');
        return new Transformer(pattern, replacer, global);
    }

    private Transformer(Pattern pattern, Function<MatchResult, String> replacer, boolean global)
    {
        this.pattern = pattern;
        this.replacer = replacer;
        this.global = global;
    }

    String transform(String input)
    {
        Matcher matcher = pattern.matcher(input);
        return global ? matcher.replaceAll(replacer) : matcher.replaceFirst(replacer);
    }

    private static int toIntFlags(String flags)
    {
        int result = 0;
        if (contains(flags, 'i'))
            result |= Pattern.CASE_INSENSITIVE;
        if (contains(flags, 'm'))
            result |= Pattern.MULTILINE;
        if (contains(flags, 's'))
            result |= Pattern.DOTALL;
        if (contains(flags, 'u'))
            result |= Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;
        return result;

    }

    private static boolean contains(String s, char c)
    {
        return s.indexOf(c) >= 0;
    }
}
