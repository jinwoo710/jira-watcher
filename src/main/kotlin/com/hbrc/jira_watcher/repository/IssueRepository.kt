package com.hbrc.jira_watcher.repository

import com.hbrc.jira_watcher.entity.Issue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface IssueRepository : JpaRepository<Issue, String> {
}