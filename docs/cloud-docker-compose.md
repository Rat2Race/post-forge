#### 클라우드 docker-compose.yml 파일
```
services:
services:
  postgres:
    image: postgres:18
    container_name: postforge-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql
    networks:
      - postforge-network
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    image: rat2hub/postforge:latest
    container_name: postforge-app
    restart: always
    ports:
      - "8080:8080"
    environment:
      SPRING_CORS_ALLOWED_ORIGINS: ${FRONT_URI}

      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}

      GMAIL_USERNAME: ${GMAIL_USERNAME}
      GMAIL_PASSWORD: ${GMAIL_PASSWORD}

      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}

      NAVER_CLIENT_ID: ${NAVER_CLIENT_ID}
      NAVER_CLIENT_SECRET: ${NAVER_CLIENT_SECRET}

      KAKAO_CLIENT_ID: ${KAKAO_CLIENT_ID}
      KAKAO_CLIENT_SECRET: ${KAKAO_CLIENT_SECRET}

      JWT_SECRET: ${JWT_SECRET}

      MONITORING_USERNAME: ${MONITORING_USERNAME}
      MONITORING_PASSWORD: ${MONITORING_PASSWORD}

      AWS_S3_ACCESS_KEY: ${AWS_S3_ACCESS_KEY}
      AWS_S3_SECRET_KEY: ${AWS_S3_SECRET_KEY}
    depends_on:
      postgres:
         condition: service_healthy
    networks:
      - postforge-network

volumes:
  postgres_data:

networks:
  postforge-network:
    driver: bridge
```
