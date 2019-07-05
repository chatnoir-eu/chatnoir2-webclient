FROM tomcat:8

RUN set -x \
    && groupadd -g 1000 tomcat \
    && useradd -g tomcat -u 1000 -M -d /usr/local/tomcat tomcat \
    && chown -R tomcat:tomcat /usr/local/tomcat \
    && mkdir /var/log/chatnoir2/ \
    && chown tomcat:tomcat /var/log/chatnoir2 \
    && chmod 700 /var/log/chatnoir2 \
    && mkdir /etc/chatnoir2/ \
    && chown tomcat:tomcat /etc/chatnoir2 \
    && chmod 700 /etc/chatnoir2 \
    && rm -r /usr/local/tomcat/webapps/*

COPY ./build/libs/chatnoir2-* /usr/local/tomcat/webapps/ROOT.war

RUN set -x \
    && apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends \
        gosu \
    && rm -rf /var/lib/apt/lists/*

COPY ./docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["catalina.sh", "run"]
