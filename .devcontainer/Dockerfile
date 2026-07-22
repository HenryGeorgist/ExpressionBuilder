FROM mcr.microsoft.com/devcontainers/java:1-21-trixie

RUN apt-get update && apt-get install -y \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxrandr2 \
    libfontconfig1 \
    libfreetype6 \
    fonts-dejavu-core \
    fonts-liberation \
    x11-utils \
    && rm -rf /var/lib/apt/lists/*