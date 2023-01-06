package com.hbrc.jira_watcher.entity

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Issue(
    @Id
    var id: String = "",
    var status: String = "",
    @Column(length = 2048)
    var title: String = "",
    var assignee: String = "",
) {

    @CreationTimestamp
    var createAt: LocalDateTime = LocalDateTime.now()

    @UpdateTimestamp
    var updateAt: LocalDateTime = LocalDateTime.now()
}