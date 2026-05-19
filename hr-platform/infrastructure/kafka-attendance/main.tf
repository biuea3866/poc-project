resource "kafka_topic" "event_hr_attendance_v1" {
  name               = "event.hr.attendance.v1"
  replication_factor = var.replication_factor
  partitions         = var.partitions

  config = {
    "retention.ms"        = "604800000" # 7 days in milliseconds
    "cleanup.policy"      = "delete"
    "compression.type"    = "lz4"
    "min.insync.replicas" = tostring(var.min_insync_replicas)
  }
}

resource "kafka_topic" "event_hr_attendance_v1_dlq" {
  name               = "event.hr.attendance.v1.dlq"
  replication_factor = var.replication_factor
  partitions         = 3

  config = {
    "retention.ms"        = "2592000000" # 30 days in milliseconds
    "cleanup.policy"      = "delete"
    "compression.type"    = "lz4"
    "min.insync.replicas" = tostring(var.min_insync_replicas)
  }
}
