package com.hbrc.jira_watcher.jandi

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.hbrc.jira_watcher.doto.IssueDto
import com.hbrc.jira_watcher.entity.Message
import com.hbrc.jira_watcher.repository.MessageRepository
import org.codehaus.jettison.json.JSONObject
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class JandiProgressMessageTasklet(
    private val messageRepository: MessageRepository,
    private val config: JandiConfig
) : Tasklet {
    private val restTemplate = RestTemplate()

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus? {
        val histories = messageRepository.findAll()
        val lastProgressHistory = histories.findLast { it.type == "PROGRESS_ISSUES" }

        if (lastProgressHistory != null) {
            val jsonObj = createProgressIssuesText(lastProgressHistory)
            val header = HttpHeaders()
            header.contentType = MediaType.APPLICATION_JSON
            val httpEntity = HttpEntity(jsonObj.toString(), header)
            restTemplate.exchange(
                config.progressBotUrl,
                org.springframework.http.HttpMethod.POST,
                httpEntity,
                String::class.java
            )


        }
        return RepeatStatus.FINISHED
    }

    fun createProgressIssuesText(message: Message): JSONObject {
        val epicIssueType = object : TypeToken<List<IssueDto.InProgressEpic>>() {}.type
        val issues = Gson().fromJson<ArrayList<IssueDto.InProgressEpic>>(message.json, epicIssueType)
        val text = StringBuilder()


        issues.forEach { issue ->
            if (issue.tasks.isNotEmpty()) {
                if (issue.id == "null")
                    text.append("\uD83D\uDFEA${issue.title}\\n")
                else
                    text.append("\uD83D\uDFEA[${issue.title}](https://hbrc.atlassian.net/browse/${issue.id})\\n")
                issue.tasks.forEach { task ->
                    text.append("    \uD83D\uDFE6[${task.id} - ${task.title}](https://hbrc.atlassian.net/browse/${task.id})\\n")
                    if (task.tasks.isEmpty())
                        text.append("     ㄴ\uD83D\uDC68\u200D\uD83D\uDCBB${task.assignee}\\n")
                    else
                        task.tasks.forEach { subtask ->
                            text.append("     ㄴ")
                            if (subtask.status == "진행 중")
                                text.append("\uD83D\uDC68\u200D\uD83D\uDCBB")
                            else
                                text.append("\uD83D\uDCA4")
                            text.append("${subtask.assignee} ${subtask.id} - ${subtask.title}\\n")
                        }
                }
            }
        }
        return JSONObject("{\"body\": \"${text}\" }")
    }
}