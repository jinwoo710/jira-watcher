package com.hbrc.jira_watcher.jandi

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.hbrc.jira_watcher.doto.IssueDto
import com.hbrc.jira_watcher.entity.Message
import com.hbrc.jira_watcher.repository.MessageRepository
import org.codehaus.jettison.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class JandiMessageSender (
    private val messageRepository: MessageRepository
){
    @Value("\${jandi.progress_bot_url}")
    lateinit var progress_url: String
    @Value("\${jandi.changed_bot_url}")
    lateinit var changed_url: String
    private val restTemplate = RestTemplate()

    fun sendMessage() {
        val histories = messageRepository.findAll()
        val lastChangedHistory = histories.findLast { it.type == "CHANGED_ISSUES" }
        val lastProgressHistory = histories.findLast { it.type == "PROGRESS_ISSUES" }

        if(lastProgressHistory != null) {
            val jsonObj = createProgressIssuesText(lastProgressHistory)
            val header= HttpHeaders()
            header.contentType = MediaType.APPLICATION_JSON
            val httpEntity = HttpEntity(jsonObj.toString(), header)
            restTemplate.exchange(progress_url,org.springframework.http.HttpMethod.POST,httpEntity, String::class.java)
        }

        if(lastChangedHistory != null) {
            val jsonObj = createChangedIssuesText(lastChangedHistory)
            val header= HttpHeaders()
            header.contentType = MediaType.APPLICATION_JSON
            val httpEntity = HttpEntity(jsonObj.toString(), header)
            restTemplate.exchange(changed_url,org.springframework.http.HttpMethod.POST,httpEntity, String::class.java)
        }
    }

    fun createProgressIssuesText(message: Message): JSONObject {
        val epicIssueType = object : TypeToken<List<IssueDto.InProgressEpic>>() {}.type
        val issues = Gson().fromJson<ArrayList<IssueDto.InProgressEpic>>(message.json, epicIssueType)
        val text = StringBuilder()


        issues.forEach { issue ->
            if (issue.tasks.isNotEmpty()) {
                if(issue.id=="null")
                    text.append("\uD83D\uDFEA${issue.title}\\n")
                else
                text.append("\uD83D\uDFEA[${issue.title}](https://hbrc.atlassian.net/browse/${issue.id})\\n")
                issue.tasks.forEach { task ->
                    text.append("    \uD83D\uDFE6[${task.id} - ${task.title}](https://hbrc.atlassian.net/browse/${task.id})\\n")
                    if(task.tasks.isEmpty())
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

    fun createChangedIssuesText(message: Message): JSONObject {
        val changedIssueType = object : TypeToken<List<IssueDto.Simple>>() {}.type
        val issues = Gson().fromJson<ArrayList<IssueDto.Simple>>(message.json, changedIssueType)
        val text = StringBuilder()

        text.append("[시작된 이슈]\\n")
        issues.forEach { issue ->
            if (issue.status == "진행 중") {
                text.append("ㄴ\uD83C\uDD95${issue.assignee} ${issue.id} ${issue.title}\\n")
            }
        }

        text.append("[해결된 이슈]\\n")
        issues.forEach { issue ->
            if (issue.status == "완료됨") {
                text.append("ㄴ\uD83C\uDD97${issue.assignee} ${issue.id} ${issue.title}\\n")
            }
        }
        text.append("[블럭된 이슈]\\n")
        issues.forEach { issue ->
            if (issue.status == "BLOCKED") {
                text.append("ㄴ⏹️${issue.assignee} ${issue.id} ${issue.title}\\n")
            }
        }
        return JSONObject("{\"body\": \"${text}\" }")
    }
}