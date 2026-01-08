package com.codinghappy.fintechai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 配置超时时间，这对 AI 接口调用非常重要！
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 连接超时设置 10秒 (连不上 DeepSeek 就报错)
        factory.setConnectTimeout(10000); 
        
        // 读取超时设置 60秒 (AI 思考生成内容比较慢，给它 1 分钟时间)
        factory.setReadTimeout(60000); 
        
        return new RestTemplate(factory);
    }
}