FROM node:latest

RUN mkdir -p /usr/app
WORKDIR /usr/app

COPY package.json package-lock.json tsconfig.json tslint.json ./
RUN npm install

COPY . .
RUN ./node_modules/.bin/tsc

CMD ["node", "./build/index.js"]
