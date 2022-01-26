package com.sjtu.yygh.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.sjtu.yygh.user.mapper")
public class UserConfig {
}
