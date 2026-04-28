# Build stage
FROM golang:1.23.0-alpine AS builder

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o /app/server ./cmd/server

# Final stage
FROM scratch

LABEL org.opencontainers.image.source=https://github.com/minhngo248/e2e-k8s-agentic-system

WORKDIR /app

COPY --from=builder /app/server /app/server

ENTRYPOINT ["/app/server"]
CMD ["-http", ":8080"]