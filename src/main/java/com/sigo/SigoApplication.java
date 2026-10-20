package com.sigo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class SigoApplication { public static void main(String[] args){ SpringApplication.run(SigoApplication.class,args); } }
