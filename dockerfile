# 1단계: 빌드 환경 (Gradle 8.5 + JDK 21)
FROM gradle:8.14-jdk21 AS build
WORKDIR /home/gradle/src
COPY . .
# 테스트 제외 빌드
RUN gradle build --no-daemon -x test

# 2단계: 실행 환경 (JDK 21 JRE)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# 빌드 결과물 복사
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# 컨테이너 실행
ENTRYPOINT ["java", "-jar", "app.jar"]