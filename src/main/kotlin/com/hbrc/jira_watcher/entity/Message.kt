package com.hbrc.jira_watcher.entity

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import javax.persistence.*

@Entity
class Message (
    var type: String = "",
    @Column(columnDefinition = "LONGTEXT")
    var json: String = ""
)
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Int = 0
    @CreationTimestamp
    var createAt: LocalDateTime = LocalDateTime.now()

    @UpdateTimestamp
    var updateAt: LocalDateTime = LocalDateTime.now()
}