variable "bootstrap_servers" {
  type        = list(string)
  description = "Kafka bootstrap servers"
  default     = ["localhost:9092"]
}

variable "environment" {
  type        = string
  description = "Environment name (dev, staging, prod)"
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod."
  }
}

variable "replication_factor" {
  type        = number
  description = "Replication factor for the topic"
  default     = 1

  validation {
    condition     = var.replication_factor >= 1
    error_message = "Replication factor must be at least 1."
  }
}

variable "min_insync_replicas" {
  type        = number
  description = "Minimum number of in-sync replicas"
  default     = 1

  validation {
    condition     = var.min_insync_replicas >= 1
    error_message = "Min in-sync replicas must be at least 1."
  }
}

variable "partitions" {
  type        = number
  description = "Number of partitions for the topic"
  default     = 12

  validation {
    condition     = var.partitions > 0
    error_message = "Partitions must be greater than 0."
  }
}
