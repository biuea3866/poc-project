resource "kafka_topic" "event_hr_employee" {
  name               = "event.hr.employee"
  replication_factor = var.replication_factor
  partitions         = var.partitions

  config = {
    "retention.ms"        = "604800000" # 7 days in milliseconds
    "cleanup.policy"      = "delete"
    "compression.type"    = "lz4"
    "min.insync.replicas" = var.min_insync_replicas
  }
}
