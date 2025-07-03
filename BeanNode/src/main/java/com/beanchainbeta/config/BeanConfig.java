package com.beanchainbeta.config;

import com.beanchainbeta.services.blockchainDB;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public blockchainDB blockchainDB() {
        return new blockchainDB(); 
    }
}
