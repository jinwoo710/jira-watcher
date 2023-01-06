package com.hbrc.jira_watcher.jira

import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import com.hbrc.jira_watcher.doto.IssueDto
import com.hbrc.jira_watcher.doto.MessageDto
import com.hbrc.jira_watcher.entity.Issue
import com.hbrc.jira_watcher.entity.Message
import com.hbrc.jira_watcher.repository.IssueRepository
import com.hbrc.jira_watcher.repository.MessageRepository
import com.atlassian.jira.rest.client.api.domain.Issue as JiraIssue
import org.codehaus.jettison.json.JSONObject
import com.google.gson.Gson
import org.springframework.stereotype.Component
import java.net.URI

@Component
class JiraCrawler (
    private val config: JiraConfig,
    private val issueRepository: IssueRepository,
    private val messageRepository: MessageRepository
) {
    fun crawlingIssues() {
        val jiraIssues = ArrayList<JiraIssue>()
        val issueEntities = ArrayList<Issue>()
        val progressEpicIssues = ArrayList<IssueDto.InProgressEpic>()
        val progressTaskIssues = ArrayList<IssueDto.InProgressTask>()
        val progressTaskIssuesWithoutEpic = ArrayList<IssueDto.InProgressTask>()
        val progressSubTaskIssues = ArrayList<IssueDto.InProgressSubTask>()

        val factory = AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(
            URI.create(config.baseURI),
            config.username,
            config.token
        )
        val projects = factory.projectClient.allProjects.claim()
        println(projects)

        projects.forEach{ project ->
                var page = 0
                val maxResult = 50
                while (true) {
                    println("${project.name} 프로젝트의 ${page}번째 페이지 크롤링 중")
                    val search = factory.searchClient.searchJql(
                        "project = ${project.name} ORDER BY created DESC, issuetype ASC",
                        maxResult,
                        maxResult * page,
                        null
                    )
                    val issues = search.claim().issues
                    issues.forEach { issue ->
                            jiraIssues.add(issue)
                            separateIssue(issue, progressEpicIssues, progressTaskIssues, progressSubTaskIssues)
                            translateIssueToEntity(issue, issueEntities)

                    }
                    if (issues.count() < maxResult) {
                        break
                    } else {
                        page += 1
                    }
                }
        }

        println("크롤링 완료")
        println("[진행 중] 및 [변경사항 메세지] 감지 중")
        reconnectIssues(progressEpicIssues, progressTaskIssues,progressTaskIssuesWithoutEpic, progressSubTaskIssues)
        detectChangedIssues(issueEntities)
        println("[진행 중] 및 [변경사항 메시지] 저장 완료")
    }

    fun separateIssue(
        issue: JiraIssue,
        progressEpicIssues: ArrayList<IssueDto.InProgressEpic>,
        progressTaskIssues: ArrayList<IssueDto.InProgressTask>,
        progressSubTaskIssues: ArrayList<IssueDto.InProgressSubTask>
    ) {
        if (issue.issueType.name == "에픽" && issue.status.name !== "완료됨" && issue.status.name !== "BLOCKED") {
            val epicIssue = IssueDto.InProgressEpic(
                issue.key,
                issue.summary,
                arrayListOf()
            )
            progressEpicIssues.add(epicIssue)
        } else if (issue.issueType.name == "작업" && issue.status.name == "진행 중") {
            val parentInfo = issue.fields.find { field -> field.name == "상위" }
            val parentId = if(parentInfo != null) {JSONObject(parentInfo?.value.toString())["key"] as String} else {null}
            val taskIssue = IssueDto.InProgressTask(
                issue.key,
                issue.summary,
                issue.assignee?.displayName ?: "미할당",
                issue.status.name,
                arrayListOf(),
                parentId
            )
            progressTaskIssues.add(taskIssue)

        } else if (issue.issueType.name == "하위 작업" && issue.status.name == "진행 중") {
            val parentInfo = issue.fields.find { field -> field.name == "상위" }
            val subTaskIssue = IssueDto.InProgressSubTask(
                issue.key,
                issue.status.name,
                issue.summary,
                issue.assignee?.displayName ?: "미할당",
                JSONObject(parentInfo?.value.toString())["key"] as String
            )
            progressSubTaskIssues.add(subTaskIssue)
        }
    }

    fun translateIssueToEntity(
        issue: JiraIssue,
        entities: ArrayList<Issue>
    ) {
        entities.add(
            IssueDto.Simple(
                issue.key,
                issue.status.name,
                issue.summary,
                issue.assignee?.displayName ?: "미할당"
            ).toEntity()
        )
    }

    fun reconnectIssues(
        progressEpicIssues: ArrayList<IssueDto.InProgressEpic>,
        progressTaskIssues: ArrayList<IssueDto.InProgressTask>,
        progressTaskIssuesWithoutEpic: ArrayList<IssueDto.InProgressTask>,
        progressSubTaskIssues: ArrayList<IssueDto.InProgressSubTask>
    ) {
        progressSubTaskIssues.forEach {
            val taskIssue = progressTaskIssues.find { taskIssue ->
                taskIssue.id == it.parent_id
            }
            taskIssue?.tasks?.add(it)
        }
        progressTaskIssues.forEach {
            if (it.tasks.isNotEmpty()) {
                val taskIssue = progressEpicIssues.find { epicIssue ->
                    epicIssue.id == it.parent_id
                }

                taskIssue?.tasks?.add(it)
            }
            if(it.parent_id == null)
                progressTaskIssuesWithoutEpic.add(it)
        }
        val dummy = IssueDto.InProgressEpic(id="null",title="에픽 설정 없음", tasks=progressTaskIssuesWithoutEpic)
        progressEpicIssues.add(dummy)

        val epicJson = Gson().toJson(progressEpicIssues).toString()
        val message: Message = MessageDto.Insert("PROGRESS_ISSUES", epicJson).toEntity()
        println("-----------------------------------------------")
println(epicJson)
        println("-----------------------------------------------")
        messageRepository.save(message)


    }

    fun detectChangedIssues(
        issueEntities: ArrayList<Issue>
    ) {
        val changedIssues = ArrayList<IssueDto.Simple>()
        val oldIssues = issueRepository.findAll()

        issueEntities.forEach { issue ->
            val foundIssue = oldIssues.find { it.id == issue.id }
            if (foundIssue == null) {
                issueRepository.save(issue)
                if (issue.status !== "해야 할 일")
                    changedIssues.add(IssueDto.Simple.from(issue))
            } else {
                if (foundIssue.status != issue.status) {
                    changedIssues.add(IssueDto.Simple.from(issue))
                    issueRepository.save(issue)
                }
            }
        }


        val changedIssuesJson = Gson().toJson(changedIssues).toString()
        val message: Message = MessageDto.Insert("CHANGED_ISSUES", changedIssuesJson).toEntity()
        messageRepository.save(message)


    }

}