package dev.iamrat.support.time;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "postforge.time")
public class PostForgeTimeProperties {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    private ZoneId zone = DEFAULT_ZONE;

    public ZoneId getZone() {
        return zone;
    }

    public void setZone(ZoneId zone) {
        this.zone = zone == null ? DEFAULT_ZONE : zone;
    }
}
