FROM eclipse-temurin:21-jdk-jammy

# Tools for VS Code server & Node 20
RUN apt-get update -y \
 && apt-get install -y curl wget tar git unzip ca-certificates gnupg \
 && mkdir -p /etc/apt/keyrings \
 && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
 && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" > /etc/apt/sources.list.d/nodesource.list \
 && apt-get update -y && apt-get install -y nodejs \
 && node --version && npm --version \
 && corepack enable || true \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Gradle cache + non-root user
ENV GRADLE_USER_HOME=/home/gradle
RUN useradd -ms /bin/bash dev \
 && mkdir -p ${GRADLE_USER_HOME} \
 && chown -R dev:dev ${GRADLE_USER_HOME}

USER dev
WORKDIR /workspace
