package com.hbrc.jira_watcher.job

import com.hbrc.jira_watcher.jandi.JandiMessageSender
import com.hbrc.jira_watcher.jira.JiraCrawler
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
    private val jiraCrawler: JiraCrawler,
    private val rabbitTemplate: RabbitTemplate,
    private val jandi : JandiMessageSender
) {
    @Bean
    fun jiraJob(): Job =
        jobBuilderFactory.get("jiraJob")
            .incrementer(RunIdIncrementer())
            .start(singleStep())
            .build()

    @Bean
    fun singleStep(): Step {
        return stepBuilderFactory["singleStep"]
            .tasklet { _: StepContribution, _: ChunkContext ->
                jiraCrawler.crawlingIssues()
               jandi.sendMessage()
                RepeatStatus.FINISHED
            }
            .build()
    }

}