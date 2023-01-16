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
class JandiChangedMessageTasklet(
    private val messageRepository: MessageRepository,
    private val config:JandiConfig
) : Tasklet {
    private val restTemplate = RestTemplate()

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus? {
        val histories = messageRepository.findAll()
        val lastChangedHistory = histories.findLast { it.type == "CHANGED_ISSUES" }

        if(lastChangedHistory != null) {
            val jsonObj = createChangedIssuesText(lastChangedHistory)
            val header= HttpHeaders()
            header.contentType = MediaType.APPLICATION_JSON
            val httpEntity = HttpEntity(jsonObj.toString(), header)
            restTemplate.exchange(config.changedBotUrl,org.springframework.http.HttpMethod.POST,httpEntity, String::class.java)
        }

        return RepeatStatus.FINISHED
    }

    fun createChangedIssuesText(message: Message): JSONObject {
        val changedIssueType = object : TypeToken<List<IssueDto.Simple>>() {}.type
        val issues = Gson().fromJson<ArrayList<IssueDto.Simple>>(message.json, changedIssueType)
        val text = StringBuilder()

        text.append("\uD83C\uDD95시작된 이슈\\n")
        issues.forEach { issue ->
            if (issue.status == "진행 중") {
                var title = issue.title.replace("\"","\'")
                text.append("ㄴ${issue.assignee} ${issue.id} ${title}\\n")
            }
        }

        text.append("\uD83C\uDD97해결된 이슈\\n")
        issues.forEach { issue ->
            if (issue.status == "완료됨") {
                var title = issue.title.replace("\"","\'")
                text.append("ㄴ${issue.assignee} ${issue.id} ${title}\\n")
            }
        }
        text.append("⏹️블럭된 이슈\\n")
        issues.forEach { issue ->
            if (issue.status == "BLOCKED") {
                var title = issue.title.replace("\"","\'")
                text.append("ㄴ${issue.assignee} ${issue.id} ${title}\\n")
            }
        }
        return JSONObject("{\"body\": \"${text}\" }")
    }
}