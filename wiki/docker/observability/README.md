# Observability Stack

## Start

```bash
docker compose up -d --build wiki-api otel-collector tempo prometheus loki promtail grafana
```

## Endpoints

- Wiki API: `http://localhost:8081`
- Actuator Prometheus: `http://localhost:8081/actuator/prometheus`
- Grafana: `http://localhost:3000` (`admin` / `admin`)
- Prometheus: `http://localhost:9090`
- Loki API: `http://localhost:3100`
- Tempo API: `http://localhost:3200`

## Notes

- Kafka producer/consumer tracing is enabled via:
  - `spring.kafka.template.observation-enabled=true`
  - `spring.kafka.listener.observation-enabled=true`
- Trace sampling is 100% for local debug:
  - `management.tracing.sampling.probability=1.0`
- Application logs are written to `/var/log/wiki/wiki-api.log` and shipped to Loki by Promtail.
