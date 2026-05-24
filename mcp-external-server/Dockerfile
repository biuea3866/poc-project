FROM python:3.12-slim

# uv 설치 (빠른 의존성 해결)
RUN pip install --no-cache-dir uv

WORKDIR /app

# 의존성 메타데이터 먼저 복사해 캐시 활용
COPY pyproject.toml ./
RUN uv pip install --system --no-cache "mcp>=1.0.0" "uvicorn>=0.30.0"

COPY server.py ./

ENV MCP_HOST=0.0.0.0
ENV MCP_PORT=9090
EXPOSE 9090

CMD ["python", "server.py"]
