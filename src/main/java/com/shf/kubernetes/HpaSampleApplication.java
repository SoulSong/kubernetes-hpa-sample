package com.shf.kubernetes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Startup class
 *
 * @author songhaifeng
 */
@SpringBootApplication
@RestController
@Slf4j
public class HpaSampleApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(HpaSampleApplication.class).web(WebApplicationType.SERVLET).build().run(args);
    }

    @GetMapping("/hello")
    public String hello() throws UnknownHostException {
        log.info("hello");
        return "Hello, i am " + InetAddress.getLocalHost().getHostName() + ".";
    }
}
