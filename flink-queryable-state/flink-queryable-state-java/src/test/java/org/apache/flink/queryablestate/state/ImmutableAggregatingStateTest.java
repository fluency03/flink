/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.queryablestate.state;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.state.AggregatingStateDescriptor;
import org.apache.flink.core.memory.DataOutputViewStreamWrapper;
import org.apache.flink.queryablestate.client.state.ImmutableAggregatingState;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link ImmutableAggregatingStateTest}.
 */
public class ImmutableAggregatingStateTest {

	private final AggregatingStateDescriptor<Long, MutableString, String> aggrStateDesc =
			new AggregatingStateDescriptor<>(
					"test",
					new SumAggr(),
					MutableString.class);

	private ImmutableAggregatingState<Long, String> aggrState;

	@Before
	public void setUp() throws Exception {
		if (!aggrStateDesc.isSerializerInitialized()) {
			aggrStateDesc.initializeSerializerUnlessSet(new ExecutionConfig());
		}

		final MutableString initValue = new MutableString();
		initValue.value = "42";

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		aggrStateDesc.getSerializer().serialize(initValue, new DataOutputViewStreamWrapper(out));

		aggrState = ImmutableAggregatingState.createState(
				aggrStateDesc,
				out.toByteArray()
		);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUpdate() {
		String value = aggrState.get();
		assertEquals("42", value);

		aggrState.add(54L);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testClear() {
		String value = aggrState.get();
		assertEquals("42", value);

		aggrState.clear();
	}

	/**
	 * Test {@link AggregateFunction} concatenating the already stored string with the long passed as argument.
	 */
	private static class SumAggr implements AggregateFunction<Long, MutableString, String> {

		private static final long serialVersionUID = -6249227626701264599L;

		@Override
		public MutableString createAccumulator() {
			return new MutableString();
		}

		@Override
		public void add(Long value, MutableString accumulator) {
			accumulator.value += ", " + value;
		}

		@Override
		public String getResult(MutableString accumulator) {
			return accumulator.value;
		}

		@Override
		public MutableString merge(MutableString a, MutableString b) {
			MutableString nValue = new MutableString();
			nValue.value = a.value + ", " + b.value;
			return nValue;
		}
	}

	private static final class MutableString {
		String value;
	}
}
