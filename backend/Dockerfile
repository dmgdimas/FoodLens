FROM golang:1.23-alpine AS builder

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o foodlens-backend ./cmd/server

FROM alpine:3.20

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/foodlens-backend ./foodlens-backend

USER appuser

EXPOSE 8000

CMD ["./foodlens-backend"]
