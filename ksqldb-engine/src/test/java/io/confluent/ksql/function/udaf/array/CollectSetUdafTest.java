/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.function.udaf.array;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableMap;
import io.confluent.ksql.function.udaf.Udaf;
import java.util.List;
import org.apache.kafka.common.Configurable;
import org.junit.Test;

public class CollectSetUdafTest {

  @Test
  public void shouldCollectDistinctInts() {
    final Udaf<Integer, List<Integer>, List<Integer>> udaf = CollectSetUdaf.createCollectSetInt();
    final Integer[] values = new Integer[] {3, 4, 5, 3};
    List<Integer> runningList = udaf.initialize();
    for (final Integer i : values) {
      runningList = udaf.aggregate(i, runningList);
    }
    assertThat(runningList, contains(3, 4, 5));
  }

  @Test
  public void shouldMergeDistinctIntsIncludingNulls() {
    final Udaf<Integer, List<Integer>, List<Integer>> udaf = CollectSetUdaf.createCollectSetInt();

    List<Integer> lhs = udaf.initialize();
    final Integer[] lhsValues = new Integer[] {1, 2, null, 3};
    for (final Integer i : lhsValues) {
      lhs = udaf.aggregate(i, lhs);
    }
    assertThat(lhs, contains(1, 2, null, 3));

    List<Integer> rhs = udaf.initialize();
    final Integer[] rhsValues = new Integer[] {2, null, 3, 4, 5, 6};
    for (final Integer i : rhsValues) {
      rhs = udaf.aggregate(i, rhs);
    }
    assertThat(rhs, contains(2, null, 3, 4, 5, 6));

    final List<Integer> merged = udaf.merge(lhs, rhs);
    assertThat(merged, contains(1, 2, null, 3, 4, 5, 6));
  }

  @Test
  public void shouldRespectSizeLimit() {
    final Udaf<Integer, List<Integer>, List<Integer>> udaf = CollectSetUdaf.createCollectSetInt();
    ((Configurable) udaf).configure(ImmutableMap.of(CollectSetUdaf.LIMIT_CONFIG, 1000));
    List<Integer> runningList = udaf.initialize();
    for (int i = 1; i < 2500; i++) {
      runningList = udaf.aggregate(i, runningList);
    }
    assertThat(runningList, hasSize(1000));
    assertThat(runningList, hasItem(1));
    assertThat(runningList, hasItem(1000));
    assertThat(runningList, not(hasItem(1001)));
  }

  @Test
  public void shouldIgnoreNegativeLimit() {
    final Udaf<Integer, List<Integer>, List<Integer>> udaf = CollectSetUdaf.createCollectSetInt();
    ((Configurable) udaf).configure(ImmutableMap.of(CollectSetUdaf.LIMIT_CONFIG, -1));

    List<Integer> runningList = udaf.initialize();
    for (int i = 1; i <= 25; i++) {
      runningList = udaf.aggregate(i, runningList);
    }

    assertThat(runningList, hasSize(25));
  }

}
