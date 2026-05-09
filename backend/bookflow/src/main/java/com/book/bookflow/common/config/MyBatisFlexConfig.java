//package com.book.bookflow.common.config;
//
//import com.mybatisflex.core.mybatis.FlexConfiguration;
//import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class MyBatisFlexConfig {
//
//    @Bean
//    public MyBatisFlexCustomizer myBatisFlexCustomizer() {
//        return configuration -> {
//            if (configuration instanceof FlexConfiguration) {
//                FlexConfiguration flexConfig = (FlexConfiguration) configuration;
//                // 开启驼峰命名转换
//                flexConfig.setMapUnderscoreToCamelCase(true);
//                // 打印 SQL 日志
//                flexConfig.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
//            }
//        };
//    }
//}
