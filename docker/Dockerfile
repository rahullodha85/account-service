FROM sd1pgo01lx.saksdirect.com/rhel7_2:4

# Add and unzip the Play app
COPY account-service-0.1.zip /opt/account-service-0.1.zip
COPY newrelic.* /opt/newrelic/
RUN unzip /opt/account-service-0.1.zip -d /opt

#adding container buddy
COPY containerbuddy /opt/containerbuddy

# Give exec permission to the start script (unzip doesn't preserve permissions)
RUN chmod 755 /opt/account-service-0.1/bin/account-service
RUN chmod 775 /opt/containerbuddy/*

# Play port to expose
EXPOSE 9800
# Play https port to expose
EXPOSE 9805

CMD nohup /opt/account-service-0.1/bin/account-service -Dhttp.port=9800 -Dhttps.port=9805 -Dpidfile.path=/dev/null -Dconfig.file=${CONFIG_FILE}
