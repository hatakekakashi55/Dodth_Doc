FROM node:20-slim

# Install LibreOffice, Java (required for some filters), and fonts
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libreoffice \
    libreoffice-writer \
    libreoffice-java-common \
    default-jre \
    qpdf \
    tesseract-ocr \
    ocrmypdf \
    fonts-liberation \
    fonts-crosextra-carlito \
    fonts-crosextra-caladea \
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Node dependencies
COPY package*.json ./
RUN npm install

# Copy the rest of the application
COPY . .

# Set environment variables
ENV NODE_ENV=production
ENV PORT=8000

# Expose the correct port
EXPOSE 8000

# Start the Node.js Express server
CMD ["node", "src/index.js"]
