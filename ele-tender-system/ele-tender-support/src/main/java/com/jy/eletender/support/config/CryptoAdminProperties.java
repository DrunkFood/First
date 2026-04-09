package com.jy.eletender.support.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "crypto-admin")
public class CryptoAdminProperties {

    private String streamKey = "bdc:decrypt:tasks";
}
