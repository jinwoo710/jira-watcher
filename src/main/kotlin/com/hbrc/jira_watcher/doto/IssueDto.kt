package com.hbrc.jira_watcher.doto

import com.hbrc.jira_watcher.entity.Issue
import javax.annotation.Nullable

interface IssueDto {

    data class InProgressEpic(
        val id: String,
        val title: String,
        val tasks: ArrayList<InProgressTask>
    )

    data class InProgressTask(
        val id: String,
        val title: String,
        val assignee: String,
        val status: String,
        val tasks: ArrayList<InProgressSubTask>,
        val parent_id: String?
    )

    data class InProgressSubTask(
        val id: String,
        val status: String,
        val title: String,
        val assignee: String,
        val parent_id: String
    )

    data class Simple(
        val id: String,
        val status: String,
        val title: String,
        val assignee: String,
    ) {
        companion object {
            fun from(issue: Issue) = with(issue) {
                Simple(id, status, title, assignee)
            }
        }

        fun toEntity(): Issue = Issue(
            id = id,
            status = status,
            title = title,
            assignee = assignee
        )
    }
}