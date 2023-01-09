package com.hbrc.jira_watcher.jobs

import com.hbrc.jira_watcher.jandi.JandiChangedMessageTasklet
import com.hbrc.jira_watcher.jandi.JandiProgressMessageTasklet
import com.hbrc.jira_watcher.jira.JiraCrawlerTasklet
import org.springframework.batch.core.Job
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
    private val jiraCralwerTasklet: JiraCrawlerTasklet,
    private val jandiProgressMessageTasklet : JandiProgressMessageTasklet,
    private val jandiChangedMessageTasklet: JandiChangedMessageTasklet
) {
    @Bean
    fun jiraJob(): Job =
        jobBuilderFactory.get("jiraJob")
            .incrementer(RunIdIncrementer())
            .start(jiraCralwerStep())
            .next(jandiProgressMessageStep())
            .next(jandiChangedMessageStep())
            .build()

    @Bean
    fun jiraCralwerStep() =
        stepBuilderFactory.get("jiraCralwerStep")
            .tasklet(jiraCralwerTasklet).build()

    @Bean
    fun jandiProgressMessageStep() =
        stepBuilderFactory.get("jandiProgressMessageStep")
            .tasklet(jandiProgressMessageTasklet).build()

    @Bean
    fun jandiChangedMessageStep() =
        stepBuilderFactory.get("jandiChangedMessageStep")
            .tasklet(jandiChangedMessageTasklet).build()
}