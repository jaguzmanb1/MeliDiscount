# ---- Build stage ----
FROM golang:1.23-alpine AS builder

WORKDIR /app

# Copiar go.mod y go.sum primero para aprovechar cache de dependencias
COPY go.mod go.sum ./
RUN go mod download

# Copiar el resto del código
COPY . .

# Compilar con binario estático
RUN go build -o items-api main.go

# ---- Run stage ----
FROM alpine:3.18

WORKDIR /app

# Copiar binario compilado
COPY --from=builder /app/items-api .

# Copiar datos generados
COPY data ./data

EXPOSE 8080

# Ejecutar
CMD ["./items-api"]
