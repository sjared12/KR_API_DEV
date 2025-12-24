FROM node:20-alpine
WORKDIR /app
COPY admin-portal/package.json .
RUN npm install --omit=dev
COPY admin-portal/server.js .
COPY admin-portal/public /usr/share/admin-portal
RUN mkdir -p /data
ENV CONFIG_PATH=/data/config.json
ENV PORT=4000
EXPOSE 4000
CMD ["node", "server.js"]
