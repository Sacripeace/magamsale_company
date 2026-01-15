# 1. 빌드 단계 (Gradle)
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
# 실행 권한 부여 및 빌드
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# 2. 실행 단계 (AWS Corretto JDK 사용 - 호환성 좋음)
FROM amazoncorretto:21
WORKDIR /app
# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]