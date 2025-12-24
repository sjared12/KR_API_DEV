FROM node:20-alpine
WORKDIR /app
COPY package.json .
RUN npm install --omit=dev
COPY server.js .
COPY public /usr/share/admin-portal
RUN mkdir -p /data
ENV CONFIG_PATH=/data/config.json
ENV PORT=4000
EXPOSE 4000
CMD ["node", "server.js"]
