package com.hbrc.jira_watcher.jira

import lombok.Getter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@Getter
@ConfigurationProperties(prefix="jira")
class JiraConfig(
    var baseURI: String ="",
    var username: String = "",
    var token: String =""
) {
}