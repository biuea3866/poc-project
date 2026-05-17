output "topic_name" {
  value       = kafka_topic.event_hr_employee.name
  description = "The name of the Kafka topic"
}

output "topic_partitions" {
  value       = kafka_topic.event_hr_employee.partitions
  description = "Number of partitions in the topic"
}

output "topic_replication_factor" {
  value       = kafka_topic.event_hr_employee.replication_factor
  description = "Replication factor of the topic"
}

output "topic_config" {
  value       = kafka_topic.event_hr_employee.config
  description = "Configuration map of the topic"
  sensitive   = false
}
