/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.commons.kstreams.serde.metricpoint

import com.expedia.www.haystack.commons.entities.encoders.{Base64Encoder, PeriodReplacementEncoder}
import com.expedia.www.haystack.commons.entities.{Interval, MetricPoint, MetricType, TagKeys}
import com.expedia.www.haystack.commons.unit.UnitTestSpec
import org.msgpack.core.MessagePack
import org.msgpack.value.ValueFactory

class MetricTankSerdeSpec extends UnitTestSpec {
  val statusFile = "/tmp/app-health.status"
  val DURATION_METRIC_NAME = "duration"
  val SERVICE_NAME = "dummy_service"
  val OPERATION_NAME = "dummy_operation"
  val TOPIC_NAME = "dummy"


  val metricTags = Map(TagKeys.OPERATION_NAME_KEY -> OPERATION_NAME,
    TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME)

  "MetricTank serde for metric point" should {

    "serialize and deserialize metric points using messagepack" in {

      Given("metric point")
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, metricTags, 80, computeCurrentTimeInSecs)
      val metricTankSerde = new MetricTankSerde()

      When("its serialized using the metricTank Serde")
      val serializedBytes = metricTankSerde.serializer().serialize(TOPIC_NAME, metricPoint)

      Then("it should be encoded as message pack")
      val unpacker = MessagePack.newDefaultUnpacker(serializedBytes)
      unpacker should not be null

      metricTankSerde.close()
    }

    "serialize metricpoint with the right metric interval if present" in {

      Given("metric point with a 5 minute interval")
      val metricTankSerde = new MetricTankSerde(new PeriodReplacementEncoder)
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, metricTags + (TagKeys.INTERVAL_KEY -> Interval.FIVE_MINUTE.name), 80, computeCurrentTimeInSecs)

      When("its serialized using the metricTank Serde")
      val serializedBytes = metricTankSerde.serializer().serialize(TOPIC_NAME, metricPoint)
      val unpacker = MessagePack.newDefaultUnpacker(serializedBytes)
      Then("it should be able to unpack the content")
      unpacker should not be null

      Then("it unpacked content should be a valid map")
      val metricData = unpacker.unpackValue().asMapValue().map()
      metricData should not be null

      Then("interval key should be set as 300 seconds")
      metricData.get(ValueFactory.newString(metricTankSerde.serializer().intervalKey)).asIntegerValue().asInt() shouldBe 300

      metricTankSerde.close()
    }

    "serialize metricpoint with the default interval if not present" in {

      Given("metric point without the interval tag")
      val metricTankSerde = new MetricTankSerde(new PeriodReplacementEncoder)
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, metricTags, 80, computeCurrentTimeInSecs)

      When("its serialized using the metricTank Serde")
      val serializedBytes = metricTankSerde.serializer().serialize(TOPIC_NAME, metricPoint)
      val unpacker = MessagePack.newDefaultUnpacker(serializedBytes)
      Then("it should be able to unpack the content")
      unpacker should not be null

      Then("it unpacked content should be a valid map")
      val metricData = unpacker.unpackValue().asMapValue().map()
      metricData should not be null

      Then("interval key should be set as default metric interval in seconds")
      metricData.get(ValueFactory.newString(metricTankSerde.serializer().intervalKey)).asIntegerValue().asInt() shouldBe metricTankSerde.serializer().DEFAULT_INTERVAL_IN_SECS

      metricTankSerde.close()
    }


    "serialize and deserialize simple metric points without loosing data" in {

      Given("metric point")
      val metricTankSerde = new MetricTankSerde(new PeriodReplacementEncoder)
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, metricTags, 80, computeCurrentTimeInSecs)

      When("its serialized in the metricTank Format")
      val serializedBytes = metricTankSerde.serializer().serialize(TOPIC_NAME, metricPoint)
      val deserializedMetricPoint = metricTankSerde.deserializer().deserialize(TOPIC_NAME, serializedBytes)

      Then("it should be encoded as message pack")
      metricPoint shouldEqual deserializedMetricPoint

      metricTankSerde.close()
    }

    "serialize and deserialize metric points with tag values containing special characters without loosing data" in {

      val tagWithSpecialCharacters = Map(TagKeys.SERVICE_NAME_KEY -> SERVICE_NAME, TagKeys.OPERATION_NAME_KEY -> "service:someOp")

      Given("metric point")
      val metricTankSerde = new MetricTankSerde(new PeriodReplacementEncoder)
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, tagWithSpecialCharacters, 80, computeCurrentTimeInSecs)

      When("its serialized in the metricTank Format")
      val serializedBytes = metricTankSerde.serializer().serialize(TOPIC_NAME, metricPoint)
      val deserializedMetricPoint = metricTankSerde.deserializer().deserialize(TOPIC_NAME, serializedBytes)

      Then("it should be encoded as message pack")
      metricPoint shouldEqual deserializedMetricPoint

      metricTankSerde.close()
    }

    "serialize and deserialize metric points with spaces or periods in operation/service names without losing information" in {

      val tagWithSpaceAndPeriod = Map(TagKeys.SERVICE_NAME_KEY -> "service.name", TagKeys.OPERATION_NAME_KEY -> "special.operation name")

      Given("metric point")
      val metricTankSerde = new MetricTankSerde(new PeriodReplacementEncoder)
      val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, tagWithSpaceAndPeriod, 80, computeCurrentTimeInSecs)

      When("its serialized in the metricTank Format")
      val serializedBytes = metricTankSerde.serializer().serialize(TOPIC_NAME, metricPoint)
      val deserializedMetricPoint = metricTankSerde.deserializer().deserialize(TOPIC_NAME, serializedBytes)

      Then("it should be encoded as message pack")
      metricPoint shouldEqual deserializedMetricPoint

      metricTankSerde.close()
    }
  }

  "serializer returns null for any exception" in {

    Given("MetricTankSerde and a null metric point")
    val metricTankSerde = new MetricTankSerde(new PeriodReplacementEncoder)
    val metricPoint = null

    When("its serialized using the metricTank Serde")
    val serializedBytes = metricTankSerde.serializer().serialize(TOPIC_NAME, metricPoint)

    Then("serializer should return null")
    serializedBytes shouldBe null
    metricTankSerde.close()
  }

  "base64 decode metricpoint names" in {
    Given("MetricTankSerde and with a base64 encoded name only")
    val metricTankSerde = new MetricTankSerde(new Base64Encoder)
    val tagWithSpaceAndPeriod = Map(TagKeys.SERVICE_NAME_KEY -> "service.name", TagKeys.OPERATION_NAME_KEY -> "special.operation name")
    val metricPoint = MetricPoint(DURATION_METRIC_NAME, MetricType.Gauge, tagWithSpaceAndPeriod, 80, computeCurrentTimeInSecs)
    val serializedBytes = metricTankSerde.serializer().serialize(TOPIC_NAME, metricPoint)

    When("its deserialized using the metricTank Serde")
    val deserializedBytes = metricTankSerde.deserializer().deserialize(TOPIC_NAME, serializedBytes)

    Then("it should be encoded as message pack")
    metricPoint shouldEqual deserializedBytes
    metricTankSerde.close()
  }
}
