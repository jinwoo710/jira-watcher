package com.hbrc.jira_watcher.entity

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import javax.persistence.*

@Entity
class Message (
    @Id
    @GeneratedValue
    var id: Int = 0,
    var type: String = "",
    @Column(length = 32768)
    var json: String = ""
)
{
    @CreationTimestamp
    var createAt: LocalDateTime = LocalDateTime.now()

    @UpdateTimestamp
    var updateAt: LocalDateTime = LocalDateTime.now()
}