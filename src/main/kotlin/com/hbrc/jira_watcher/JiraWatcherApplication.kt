package com.hbrc.jira_watcher

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableBatchProcessing
class JiraWatcherApplication

fun main(args: Array<String>) {
	runApplication<JiraWatcherApplication>(*args)
}
