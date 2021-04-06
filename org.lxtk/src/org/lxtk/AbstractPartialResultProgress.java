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
package org.lxtk;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.lxtk.jsonrpc.DefaultGson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Partial implementation of the {@link PartialResultProgress} interface.
 * <p>
 * This class is expected to have only direct subclasses, i.e. every direct subclass of this class
 * must be final (an anonymous class is always implicitly final).
 * </p>
 *
 * @param <T> the type of the partial result
 */
public abstract class AbstractPartialResultProgress<T>
    extends AbstractProgress
    implements PartialResultProgress
{
    private final Type type;
    private Gson gson = DefaultGson.INSTANCE;

    /**
     * Constructs a partial result progress instance with a generated token.
     */
    public AbstractPartialResultProgress()
    {
        this(Either.forLeft(UUID.randomUUID().toString()));
    }

    /**
     * Constructs a partial result progress instance with the given token.
     *
     * @param token not <code>null</code>
     */
    public AbstractPartialResultProgress(Either<String, Integer> token)
    {
        super(token);
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType))
            throw new IllegalStateException("Missing type parameter"); //$NON-NLS-1$
        type = ((ParameterizedType)superclass).getActualTypeArguments()[0];
    }

    /**
     * Returns the type of the partial result.
     *
     * @return the partial result type (never <code>null</code>)
     */
    public final Type getType()
    {
        return type;
    }

    /**
     * Sets the {@link Gson} instance to use when deserializing the partial result.
     *
     * @param gson not <code>null</code>
     */
    public final void setGson(Gson gson)
    {
        this.gson = Objects.requireNonNull(gson);
    }

    @Override
    protected final void doAccept(ProgressParams params)
    {
        Object value = params.getValue().getRight();
        if (!(value instanceof JsonElement))
            return;

        onAccept(gson.fromJson(((JsonElement)value), type));
    }

    /**
     * Handles the next part of the partial result.
     *
     * @param value never <code>null</code>
     */
    protected abstract void onAccept(T value);
}
