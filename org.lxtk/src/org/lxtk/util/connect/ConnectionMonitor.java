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
package org.lxtk.util.connect;

import java.time.Duration;
import java.util.Objects;

/**
 * A connection monitor is a {@link Runnable} that runs until a given connection
 * is closed, periodically checking the connection state.
 */
public final class ConnectionMonitor
    implements Runnable
{
    private final Connection connection;
    private long sleepDuration = 100; // ms

    /**
     * Constructor.
     *
     * @param connection not <code>null</code>
     */
    public ConnectionMonitor(Connection connection)
    {
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * Sets the sleep duration for this monitor. This is only necessary in case
     * a default value (100 ms) is not appropriate.
     *
     * @param sleepDuration a positive duration
     */
    public void setSleepDuration(Duration sleepDuration)
    {
        if (sleepDuration.isNegative() || sleepDuration.isZero())
            throw new IllegalArgumentException();
        this.sleepDuration = sleepDuration.toMillis();
    }

    @Override
    public void run()
    {
        while (!connection.isClosed())
        {
            try
            {
                Thread.sleep(sleepDuration);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }
    }
}
