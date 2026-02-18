# Observability Stack

## Start

```bash
docker compose up -d --build \
  wiki-api otel-collector tempo prometheus loki promtail grafana \
  node-exporter cadvisor mysqld-exporter kafka-exporter kafka-jmx-exporter
```

## Endpoints

- Wiki API: `http://localhost:8081`
- Actuator Prometheus: `http://localhost:8081/actuator/prometheus`
- Grafana: `http://localhost:3000` (`admin` / `admin`)
- Prometheus: `http://localhost:9090`
- Loki API: `http://localhost:3100`
- Tempo API: `http://localhost:3200`
- Node Exporter: `http://localhost:9100/metrics`
- cAdvisor: `http://localhost:8088/metrics`
- MySQL Exporter: `http://localhost:9104/metrics`
- Kafka Exporter: `http://localhost:9308/metrics`
- Kafka JMX Exporter: `http://localhost:5556/metrics`

## Notes

- Kafka producer/consumer tracing is enabled via:
  - `spring.kafka.template.observation-enabled=true`
  - `spring.kafka.listener.observation-enabled=true`
- Trace sampling is 100% for local debug:
  - `management.tracing.sampling.probability=1.0`
- Application logs are written to `/var/log/wiki/wiki-api.log` and shipped to Loki by Promtail.
- Pre-provisioned Grafana dashboards:
  - `Wiki API - Application & JVM`
  - `Infra - Host & Containers`
  - `MySQL Metrics`
  - `Kafka & Kafka JVM`

## Prometheus quick checks

- `up`
- `jvm_threads_live_threads`
- `process_cpu_usage`
- `mysql_global_status_threads_connected`
- `kafka_consumergroup_lag`
- `kafka_jvm_threads_threadcount`
- `container_memory_usage_bytes{name!=""}`
