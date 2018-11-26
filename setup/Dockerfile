FROM mongo

WORKDIR /usr/src/configs

COPY replicaSet.js .
COPY setup.sh .

RUN ["chmod", "+x", "/usr/src/configs/setup.sh"]

CMD ["./setup.sh"]
