package cn.gemrun.base.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目的启动类
 *
 *
 * @author Gemrun
 */
@SuppressWarnings("SpringComponentScan") // 忽略 IDEA 无法识别 ${base.info.base-package}
@SpringBootApplication(scanBasePackages = {"${base.info.base-package}.server", "${base.info.base-package}.module"})
public class BaseServerApplication {

    public static void main(String[] args) {


        SpringApplication.run(BaseServerApplication.class, args);
//        new SpringApplicationBuilder(BaseServerApplication.class)
//                .applicationStartup(new BufferingApplicationStartup(20480))
//                .run(args);


    }

}
