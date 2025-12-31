package dev.iamrat.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class AuditingFields {

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@CreatedBy
	@Column(nullable = false, updatable = false, length = 100)
	private String createdBy;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime modifiedAt;

	@LastModifiedBy
	@Column(nullable = false, length = 100)
	private String modifiedBy;

}