package com.microsoft.azure.cosmosdb.kafka.connect.sink

import java.util.Properties
import java.util.UUID.randomUUID

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.microsoft.azure.cosmosdb.kafka.connect.config.CosmosDBConfigConstants
import com.microsoft.azure.cosmosdb.kafka.connect.kafka.KafkaCluster
import com.microsoft.azure.cosmosdb.kafka.connect.model.CosmosDBDocumentTest
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.azure.cosmosdb.kafka.connect.config.TestConfigurations
import org.apache.kafka.connect.runtime.distributed.DistributedConfig
import org.apache.kafka.connect.runtime.{ConnectorConfig, WorkerConfig}


object SourceConnectReaderTest {

  def main(args: Array[String]): Unit = {
    val workerProperties: Properties = TestConfigurations.getSourceWorkerProperties(KafkaCluster.BrokersList.toString)
    val connectorProperties: Properties = TestConfigurations.getSourceConnectorProperties()
    KafkaCluster.startEmbeddedConnect(workerProperties, List(connectorProperties))
    if (KafkaCluster.kafkaConnectEnabled) {
      println("Kafka Connector Enabled")
    }

    // Write 20 messages to the kafka topic to be consumed
    val producerProps: Properties = TestConfigurations.getProducerProperties(KafkaCluster.BrokersList.toString)
    val producer = new KafkaProducer[Nothing, JsonNode](producerProps)
    val testUUID = randomUUID()

    val objectMapper: ObjectMapper = new ObjectMapper

    //schema JSON test
    for (i <- 1 to 4)  {
      val json = scala.io.Source.fromFile(getClass.getResource(s"/test$i.json").toURI.getPath).mkString
      val mapper = new ObjectMapper
      val jsonNode: JsonNode =  mapper.readTree(json)
      producer.send(new ProducerRecord[Nothing, JsonNode](TestConfigurations.TOPIC, jsonNode))

    }

    //schema-less JSON test
    for (i <- 5 to 8)  {
      val json = scala.io.Source.fromFile(getClass.getResource(s"/test$i.json").toURI.getPath).mkString
      val mapper = new ObjectMapper
      val jsonNode: JsonNode =  mapper.readTree(json)
      producer.send(new ProducerRecord[Nothing, JsonNode](TestConfigurations.TOPIC, jsonNode))

    }

    // JSON string test no schema
    for (i <- 9 until 15) {
      val message = new CosmosDBDocumentTest(s"$i", s"message $i", testUUID)
      val jsonNode: JsonNode = objectMapper.valueToTree(message)

      println("sending message: ", jsonNode.findPath("id"))
      producer.send(new ProducerRecord[Nothing, JsonNode](TestConfigurations.TOPIC, jsonNode))
    }

    producer.flush()
    producer.close()
  }
}