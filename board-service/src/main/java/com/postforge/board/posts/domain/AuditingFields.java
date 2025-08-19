package com.postforge.board.posts.domain;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class AuditingFields {

	@CreatedDate
	private LocalDateTime createdAt;

	@CreatedBy
	private String createdBy;

	@LastModifiedDate
	private LocalDateTime modifiedAt;

	@LastModifiedBy
	private String modifiedBy;

}