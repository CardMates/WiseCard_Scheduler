FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y \
    wget unzip gnupg2 ca-certificates curl \
    libx11-6 libnss3 libxss1 libappindicator3-1 libasound2t64 fonts-liberation \
    libgbm-dev libxshmfence1 libatk-bridge2.0-0 libgtk-3-0 \
    && mkdir -p /usr/share/keyrings \
    && curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google-linux-signing-keyring.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-linux-signing-keyring.gpg] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update && apt-get install -y google-chrome-stable \
    && mkdir -p /tmp/chrome-user-data && chmod -R 777 /tmp/chrome-user-data \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY build/libs/scheduler-0.0.1-SNAPSHOT.jar app.jar

# Chrome 위치 환경 변수
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROMEDRIVER_DIR=/usr/bin/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]