package com.hbrc.jira_watcher.jandi

import lombok.Getter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@Getter
@ConfigurationProperties(prefix="jandi")
class JandiConfig (
    var progressBotUrl: String ="",
    var changedBotUrl: String = "",
){}