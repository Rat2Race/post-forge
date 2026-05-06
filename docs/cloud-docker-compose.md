#### 클라우드 docker-compose.yml 파일
```
services:
  postgres:
    image: pgvector/pgvector:0.8.2-pg18-trixie
    container_name: postforge-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql
      - ./docker/postgres/init:/docker-entrypoint-initdb.d
    networks:
      - postforge-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: postforge-redis
    restart: unless-stopped
    networks:
      - postforge-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  app:
    image: rat2hub/postforge:latest
    container_name: postforge-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    env_file:
      - .env
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - postforge-network

volumes:
  postgres_data:

networks:
  postforge-network:
    driver: bridge
```
