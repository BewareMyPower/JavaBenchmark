/**
 * Copyright 2025 Yunze Xu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bewaremypower.topic;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.common.naming.NamespaceName;
import org.apache.pulsar.common.naming.TopicDomain;

@Getter
public class TopicName {

  public static final String PUBLIC_TENANT = "public";
  public static final String DEFAULT_NAMESPACE = "default";

  public static final String PARTITIONED_TOPIC_SUFFIX = "-partition-";

  private final String completeTopicName;

  private final TopicDomain domain;
  private final String tenant;
  private final String namespacePortion;
  private final String localName;
  private final NamespaceName namespaceName;

  private final int partitionIndex;

  public TopicName(String completeTopicName) {
    try {
      // The topic name can be in two different forms, one is fully qualified topic name,
      // the other one is short topic name
      int index = completeTopicName.indexOf("://");
      if (index < 0) {
        // The short topic name can be:
        // - <topic>
        // - <tenant>/<namespace>/<topic>
        List<String> parts = splitBySlash(completeTopicName, 0);
        this.domain = TopicDomain.persistent;
        if (parts.size() == 3) {
          this.tenant = parts.get(0);
          this.namespacePortion = parts.get(1);
          this.localName = parts.get(2);
        } else if (parts.size() == 1) {
          this.tenant = PUBLIC_TENANT;
          this.namespacePortion = DEFAULT_NAMESPACE;
          this.localName = parts.get(0);
        } else {
          throw new IllegalArgumentException(
              "Invalid short topic name '"
                  + completeTopicName
                  + "', it should be in the format of "
                  + "<tenant>/<namespace>/<topic> or <topic>");
        }
        this.completeTopicName =
            domain.name() + "://" + tenant + "/" + namespacePortion + "/" + localName;
      } else {
        this.domain = TopicDomain.getEnum(completeTopicName.substring(0, index));
        List<String> parts = splitBySlash(completeTopicName.substring(index + "://".length()), 4);
        if (parts.size() == 4) {
          throw new IllegalArgumentException(
              "V1 topic names (with cluster component) are no longer supported. "
                  + "Please use the V2 format: '<domain>://tenant/namespace/topic'. Got: "
                  + completeTopicName);
        } else if (parts.size() != 3) {
          throw new IllegalArgumentException("Invalid topic name " + completeTopicName);
        }
        this.tenant = parts.get(0);
        this.namespacePortion = parts.get(1);
        this.localName = parts.get(2);
        this.completeTopicName = completeTopicName;
      }

      if (StringUtils.isBlank(localName)) {
        throw new IllegalArgumentException(
            String.format(
                "Invalid topic name: %s. Topic local name must not" + " be blank.",
                completeTopicName));
      }
      this.partitionIndex = getPartitionIndex(localName);
      this.namespaceName = NamespaceName.get(tenant, namespacePortion);
    } catch (NullPointerException e) {
      throw new IllegalArgumentException("Invalid topic name: " + completeTopicName, e);
    }
  }

  /**
   * @return partition index of the completeTopicName. It returns -1 if the completeTopicName
   *     (topic) is not partitioned.
   */
  public static int getPartitionIndex(String topic) {
    int partitionIndex = -1;
    if (topic.contains(PARTITIONED_TOPIC_SUFFIX)) {
      try {
        String idx = StringUtils.substringAfterLast(topic, PARTITIONED_TOPIC_SUFFIX);
        partitionIndex = Integer.parseInt(idx);
        if (partitionIndex < 0) {
          // for the "topic-partition--1"
          partitionIndex = -1;
        } else if (StringUtils.length(idx) != String.valueOf(partitionIndex).length()) {
          // for the "topic-partition-01"
          partitionIndex = -1;
        }
      } catch (NumberFormatException nfe) {
        // ignore exception
      }
    }

    return partitionIndex;
  }

  private static List<String> splitBySlash(String topic, int limit) {
    final List<String> tokens = new ArrayList<>(3);
    final int loopCount = (limit <= 0) ? Integer.MAX_VALUE : limit - 1;
    int beginIndex = 0;
    for (int i = 0; i < loopCount; i++) {
      final int endIndex = topic.indexOf('/', beginIndex);
      if (endIndex < 0) {
        tokens.add(topic.substring(beginIndex));
        return tokens;
      } else if (endIndex > beginIndex) {
        tokens.add(topic.substring(beginIndex, endIndex));
      } else {
        throw new IllegalArgumentException("Invalid topic name " + topic);
      }
      beginIndex = endIndex + 1;
    }
    if (beginIndex >= topic.length()) {
      throw new IllegalArgumentException("Invalid topic name " + topic);
    }
    tokens.add(topic.substring(beginIndex));
    return tokens;
  }
}
