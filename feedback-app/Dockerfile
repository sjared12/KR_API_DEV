
# syntax=docker/dockerfile:1

# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* yarn.lock* ./
RUN npm ci --only=production || npm install --omit=dev
COPY . .
# If you have a build step (e.g., npm run build), add it here:
# RUN npm run build

# Runtime stage
FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/server.js ./server.js
COPY --from=build /app/public ./public
EXPOSE 3000
CMD ["node", "server.js"]
