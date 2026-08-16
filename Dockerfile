FROM eclipse-temurin:21-jre
ENV JAR_NAME=billingscheduler.jar
ADD build/libs/$JAR_NAME $JAR_NAME
CMD java $JAVA_OPTS -jar $JAR_NAME