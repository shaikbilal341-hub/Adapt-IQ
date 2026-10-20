# ==============================================================================
# Adapt-IQ Research Platform - Backend Production Dockerfile
# Multi-stage optimized build for PostgreSQL + Prisma + Express
# ==============================================================================

FROM node:20-alpine AS builder

WORKDIR /app

# Install build dependencies
COPY package*.json ./
COPY tsconfig.json ./
COPY prisma ./prisma/

RUN npm ci

# Generate Prisma Client
RUN npx prisma generate

# Copy source code and compile TypeScript
COPY src ./src
RUN npm run build

# Prune dev dependencies for production
RUN npm prune --production

# ------------------------------------------------------------------------------
# Production Runtime Stage
# ------------------------------------------------------------------------------
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=4000

# Install OpenSSL for Prisma engine compatibility on Alpine
RUN apk add --no-cache openssl

# Copy package files and production node_modules
COPY package*.json ./
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/prisma ./prisma

EXPOSE 4000

# Run database migration deployment and start server
CMD ["sh", "-c", "npx prisma migrate deploy && node dist/server.js"]
