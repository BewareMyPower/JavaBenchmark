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

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.common.naming.NamespaceName;
import org.apache.pulsar.common.naming.TopicDomain;

/**
 * This class is migrated from
 * https://github.com/apache/pulsar/commit/6ce15e13cc37bb1cd9ceae1814e659c420f9550b, except for the
 * new "segment" and "topic" domains.
 */
@Getter
public class LegacyTopicName {

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

  public LegacyTopicName(String completeTopicName) {
    try {
      // The topic name can be in two different forms, one is fully qualified topic name,
      // the other one is short topic name
      if (!completeTopicName.contains("://")) {
        // The short topic name can be:
        // - <topic>
        // - <tenant>/<namespace>/<topic>
        String[] parts = StringUtils.split(completeTopicName, '/');
        if (parts.length == 3) {
          completeTopicName = TopicDomain.persistent.name() + "://" + completeTopicName;
        } else if (parts.length == 1) {
          completeTopicName =
              TopicDomain.persistent.name()
                  + "://"
                  + PUBLIC_TENANT
                  + "/"
                  + DEFAULT_NAMESPACE
                  + "/"
                  + parts[0];
        } else {
          throw new IllegalArgumentException(
              "Invalid short topic name '"
                  + completeTopicName
                  + "', it should be in the format of "
                  + "<tenant>/<namespace>/<topic> or <topic>");
        }
      }

      // Expected format: persistent://tenant/namespace/topic
      List<String> parts = Splitter.on("://").limit(2).splitToList(completeTopicName);
      this.domain = TopicDomain.getEnum(parts.get(0));

      String rest = parts.get(1);

      // Scalable topic domains (topic://, segment://) only support the new format
      // and local names may contain '/', so use limit(3) to keep the rest as localName.
      int splitLimit = 4;
      parts = Splitter.on("/").limit(splitLimit).splitToList(rest);
      if (parts.size() == 4) {
        throw new IllegalArgumentException(
            "V1 topic names (with cluster component) are no longer supported. "
                + "Please use the V2 format: '<domain>://tenant/namespace/topic'. Got: "
                + completeTopicName);
      } else if (parts.size() == 3) {
        this.tenant = parts.get(0);
        this.namespacePortion = parts.get(1);
        this.localName = parts.get(2);
        this.partitionIndex = getPartitionIndex(completeTopicName);
        this.namespaceName = NamespaceName.get(tenant, namespacePortion);
      } else {
        throw new IllegalArgumentException("Invalid topic name: " + completeTopicName);
      }

      if (StringUtils.isBlank(localName)) {
        throw new IllegalArgumentException(
            String.format(
                "Invalid topic name: %s. Topic local name must not" + " be blank.",
                completeTopicName));
      }

    } catch (NullPointerException e) {
      throw new IllegalArgumentException("Invalid topic name: " + completeTopicName, e);
    }
    this.completeTopicName =
        String.format("%s://%s/%s/%s", domain, tenant, namespacePortion, localName);
  }

  @Override
  public String toString() {
    return completeTopicName;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LegacyTopicName) {
      LegacyTopicName other = (LegacyTopicName) obj;
      return Objects.equals(completeTopicName, other.completeTopicName);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return completeTopicName.hashCode();
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
}
