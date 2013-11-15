/*
 * Copyright (c) 2008 - 2013 MongoDB Inc., Inc. <http://mongodb.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

/**
 * 
 * @since 2.12
 */
public class AggregationOptions {
    public static class Builder {

	private Integer batchSize;
	private Boolean allowDiskUsage;
	private OutputMode outputMode = OutputMode.INLINE;
	private long maxTimeMS;

	private Builder() {
	}

	public Builder allowDiskUsage(final Boolean allow) {
	    allowDiskUsage = allow;
	    return this;
	}

	public Builder batchSize(final Integer size) {
	    batchSize = size;
	    return this;
	}

	public AggregationOptions build() {
	    return new AggregationOptions(this);
	}

	/**
	 * Sets the maximum execution time for the aggregation command.
	 * 
	 * @param maxTime
	 *            the max time
	 * @param timeUnit
	 *            the time unit
	 * @return this
	 */
	public Builder maxTime(final long maxTime, final TimeUnit timeUnit) {
	    maxTimeMS = MILLISECONDS.convert(maxTime, timeUnit);
	    return this;
	}

	public Builder outputMode(final OutputMode mode) {
	    outputMode = mode;
	    return this;
	}
    }

    public enum OutputMode {
	/**
	 * The output of the aggregate operation is returned inline.
	 */
	INLINE,
	/**
	 * The output of the aggregate operation is returned using a cursor.
	 */
	CURSOR
    }

    public static Builder builder() {
	return new Builder();
    }

    private final Integer batchSize;

    private final Boolean allowDiskUsage;

    private final OutputMode outputMode;

    private final long maxTimeMS;

    AggregationOptions(final Builder builder) {
	batchSize = builder.batchSize;
	allowDiskUsage = builder.allowDiskUsage;
	outputMode = builder.outputMode;
	maxTimeMS = builder.maxTimeMS;
    }

    /**
     * If true, this enables external sort capabilities otherwise $sort produces
     * an error if the operation consumes 10 percent or more of RAM.
     */
    public Boolean getAllowDiskUsage() {
	return allowDiskUsage;
    }

    /**
     * The size of batches to use when iterating over results.
     */
    public Integer getBatchSize() {
	return batchSize;
    }

    /**
     * Gets the maximum execution time for the aggregation command.
     * 
     * @param timeUnit
     *            the time unit for the result
     * @return the max time
     */
    public long getMaxTime(final TimeUnit timeUnit) {
	return timeUnit.convert(maxTimeMS, MILLISECONDS);
    }

    /**
     * The mode of output for this configuration.
     * 
     * @see OutputMode
     */
    public OutputMode getOutputMode() {
	return outputMode;
    }

    @Override
    public String toString() {
	return "AggregationOptions{" + "batchSize=" + batchSize
		+ ", allowDiskUsage=" + allowDiskUsage + ", outputMode="
		+ outputMode + ", maxTimeMS=" + maxTimeMS + '}';
    }
}
