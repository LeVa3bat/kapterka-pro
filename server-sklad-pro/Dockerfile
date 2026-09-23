FROM node:22-alpine

WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev --ignore-scripts --no-audit --no-fund

COPY server.js ./

ENV NODE_ENV=production
ENV PORT=8080

USER node

CMD ["node", "server.js"]
