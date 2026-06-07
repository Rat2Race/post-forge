package dev.iamrat.board.view.application;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "postforge.view-count")
public class ViewCountProperties {

    @NotNull
    private ViewCountMode mode = ViewCountMode.REDIS;

    public String cacheStateLabel() {
        return mode.cacheStateLabel();
    }
}
