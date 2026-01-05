FROM node:20-alpine

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci --only=production

# Copy source
COPY tsconfig.json ./
COPY src ./src

# Build
RUN npm run build

# Expose port
EXPOSE 8081

# Initialize DB and start server
CMD ["sh", "-c", "npm run db:init && npm start"]
