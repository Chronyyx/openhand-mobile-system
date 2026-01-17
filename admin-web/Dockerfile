FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --loglevel=info

COPY . .

EXPOSE 3000

CMD ["npm", "run", "dev", "--", "--host"]
