package com.hbrc.jira_watcher.jobs

import org.springframework.batch.core.JobExecutionException
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component



@Component
@EnableScheduling
class JobSchedular(
    private val jobLauncher: JobLauncher,
    private val jobConfiguration: JobConfiguration
) {
    @Scheduled(cron = "0 30 8  * * 1-5", zone = "Asia/Seoul")
    fun runJob() {
        val param = JobParametersBuilder().addString("jiraJob", System.currentTimeMillis().toString())
            .toJobParameters()
        try {
            jobLauncher.run(jobConfiguration.jiraJob(),param)
        } catch (e: JobExecutionException) {
            println("Could not run job")
        }
    }
}