#### 클라우드 docker-compose.yml 파일
```
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
      - postgres_data:/var/lib/postfresql/data
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
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}

      SPRING_CORS_ALLOWED_ORIGINS: ${FRONT_URI}

      GMAIL_USERNAME: ${GMAIL_USERNAME}
      GMAIL_PASSWORD: ${GMAIL_PASSWORD}

      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      GOOGLE_REDIRECT_URI: ${GOOGLE_REDIRECT_URI}

      NAVER_CLIENT_ID: ${NAVER_CLIENT_ID}
      NAVER_CLIENT_SECRET: ${NAVER_CLIENT_SECRET}
      NAVER_REDIRECT_URI: ${NAVER_REDIRECT_URI}

      KAKAO_CLIENT_ID: ${KAKAO_CLIENT_ID}
      KAKAO_CLIENT_SECRET: ${KAKAO_CLIENT_SECRET}
      KAKAO_REDIRECT_URI: ${KAKAO_REDIRECT_URI}

      JWT_SECRET: ${JWT_SECRET}
      JWT_ACCESS_TOKEN_VALIDITY: ${JWT_ACCESS_TOKEN_VALIDITY:-15}
      JWT_REFRESH_TOKEN_VALIDITY: ${JWT_REFRESH_TOKEN_VALIDITY:-7}
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
