output "topic_name" {
  value       = kafka_topic.event_hr_auth_v1.name
  description = "The name of the Kafka topic"
}

output "topic_partitions" {
  value       = kafka_topic.event_hr_auth_v1.partitions
  description = "Number of partitions in the topic"
}

output "topic_replication_factor" {
  value       = kafka_topic.event_hr_auth_v1.replication_factor
  description = "Replication factor of the topic"
}

output "topic_config" {
  value       = kafka_topic.event_hr_auth_v1.config
  description = "Configuration map of the topic"
  sensitive   = false
}

output "dlq_topic_name" {
  value       = kafka_topic.event_hr_auth_v1_dlq.name
  description = "The name of the DLQ topic"
}

output "dlq_topic_partitions" {
  value       = kafka_topic.event_hr_auth_v1_dlq.partitions
  description = "Number of partitions in the DLQ topic"
}

output "dlq_topic_config" {
  value       = kafka_topic.event_hr_auth_v1_dlq.config
  description = "Configuration map of the DLQ topic"
  sensitive   = false
}
