package dev.iamrat.crawl.pipeline.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "crawl.pipeline")
public class PipelineConfig {

    private boolean enabled = true;
    private boolean killSwitch = false;
    private int dailyCap = 10;
    private String candidateCron;
    private String publishCron;

}
