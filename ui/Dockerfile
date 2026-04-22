FROM python:3.12-slim

LABEL org.opencontainers.image.source=https://github.com/minhngo248/e2e-k8s-agentic-system

RUN apt-get update && apt-get install -y --no-install-recommends \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 1001 appgroup \
    && useradd -u 1001 -g appgroup -s /bin/bash appuser
WORKDIR /app
COPY . .

RUN chown -R appuser:appgroup /app \
    && pip3 install -r requirements.txt

ENTRYPOINT ["streamlit", "run", "main.py", "--server.port=8501", "--server.address=0.0.0.0"]