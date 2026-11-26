package com.example.mymatzip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * ============================================================================
 *  파일명 : MyMatzipApplication.java
 *  역할   : 애플리케이션 시작 클래스. main() 이 실행되면 스프링 컨테이너와 내장 Tomcat 이 켜진다.
 *           포트는 application.properties 의 server.port(8082). 배포 서버는 nginx 가 앞에서 받아
 *           내부 포트(5000, cd.yml 의 접속 확인 주소)로 넘긴다.
 *  배포   : ./gradlew bootJar 로 만든 build/libs/my-matzip.jar 를 java -jar 로 실행한다.
 *           EC2 는 GitHub Actions(cd.yml)가 jar 를 S3 에 올리고, systemd(matzip.service)가 내려받은 jar 를 실행한다.
 * ============================================================================
 */

// @SpringBootApplication = 아래 3가지를 한 번에 켜는 어노테이션
//   @SpringBootConfiguration : 이 클래스도 설정 클래스로 등록
//   @EnableAutoConfiguration : build.gradle 의 라이브러리를 보고 DataSource·Thymeleaf·Security 등을 자동 설정
//   @ComponentScan           : com.example.mymatzip 아래의 @Controller·@Service·@Repository·@Component 를 찾아
//                              스프링 컨테이너(DefaultListableBeanFactory 객체)에 등록
@SpringBootApplication
public class MymatzipApplication {

	public static void main(String[] args) {
		// SpringApplication.run(): 스프링 컨테이너를 만들고, 자동 설정을 적용하고, 내장 Tomcat 을 시작한다.
		//   서버 준비가 끝나면 CommandLineRunner(DataInitializer 등)의 run() 을 호출한다.
		//   (DataInitializer: data.sql 로 들어간 평문 비밀번호를 BCrypt 로 암호화)
		//   스케줄러(@EnableScheduling)는 ReviewModerationScheduler 에서 켠다 (의심 리뷰 일괄 AI 검사)
		SpringApplication.run(MymatzipApplication.class, args);
	}
}