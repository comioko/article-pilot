package github.comioko.articlepilot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//启用aop
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@MapperScan("github.comioko.articlepilot.mapper")
public class ArticlePilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticlePilotApplication.class, args);
    }

}
