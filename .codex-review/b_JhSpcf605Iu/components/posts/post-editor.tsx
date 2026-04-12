"use client"

import { useState, useRef } from "react"
import { X, Upload, FileText, Image as ImageIcon, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

interface Attachment {
  id: string
  name: string
  type: "image" | "pdf"
  size: string
  uploading?: boolean
  progress?: number
}

interface PostEditorProps {
  initialData?: {
    title: string
    content: string
    tags: string[]
    attachments: Attachment[]
  }
  onSubmit?: (data: {
    title: string
    content: string
    tags: string[]
    attachmentIds: string[]
  }) => void
  isSubmitting?: boolean
}

const ALLOWED_TYPES = [
  "image/jpeg",
  "image/jpg",
  "image/png",
  "image/gif",
  "application/pdf",
]
const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

export function PostEditor({
  initialData,
  onSubmit,
  isSubmitting,
}: PostEditorProps) {
  const [title, setTitle] = useState(initialData?.title || "")
  const [content, setContent] = useState(initialData?.content || "")
  const [tags, setTags] = useState<string[]>(initialData?.tags || [])
  const [tagInput, setTagInput] = useState("")
  const [attachments, setAttachments] = useState<Attachment[]>(
    initialData?.attachments || []
  )
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleAddTag = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault()
      const tag = tagInput.trim().replace(",", "")
      if (tag && !tags.includes(tag) && tags.length < 10) {
        setTags([...tags, tag])
        setTagInput("")
      }
    }
  }

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((t) => t !== tagToRemove))
  }

  const handleFileSelect = async (files: FileList | null) => {
    if (!files) return

    for (const file of Array.from(files)) {
      if (!ALLOWED_TYPES.includes(file.type)) {
        alert("지원하지 않는 파일 형식입니다. (jpg, png, gif, pdf만 가능)")
        continue
      }

      if (file.size > MAX_FILE_SIZE) {
        alert("파일 크기는 10MB를 초과할 수 없습니다.")
        continue
      }

      const id = `temp-${Date.now()}-${Math.random()}`
      const newAttachment: Attachment = {
        id,
        name: file.name,
        type: file.type.startsWith("image/") ? "image" : "pdf",
        size: formatFileSize(file.size),
        uploading: true,
        progress: 0,
      }

      setAttachments((prev) => [...prev, newAttachment])

      // Simulate upload progress
      for (let progress = 0; progress <= 100; progress += 20) {
        await new Promise((r) => setTimeout(r, 200))
        setAttachments((prev) =>
          prev.map((a) => (a.id === id ? { ...a, progress } : a))
        )
      }

      // Complete upload
      setAttachments((prev) =>
        prev.map((a) =>
          a.id === id
            ? { ...a, uploading: false, id: `file-${Date.now()}` }
            : a
        )
      )
    }
  }

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes}B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
  }

  const handleRemoveAttachment = (id: string) => {
    setAttachments(attachments.filter((a) => a.id !== id))
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragOver(false)
    handleFileSelect(e.dataTransfer.files)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit?.({
      title,
      content,
      tags,
      attachmentIds: attachments.filter((a) => !a.uploading).map((a) => a.id),
    })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Title */}
      <div className="space-y-2">
        <Label htmlFor="title">제목</Label>
        <Input
          id="title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="분석 제목을 입력하세요"
          className="text-lg font-medium"
          required
        />
      </div>

      {/* Tags */}
      <div className="space-y-2">
        <Label htmlFor="tags">태그</Label>
        <div className="flex flex-wrap gap-2 mb-2">
          {tags.map((tag) => (
            <Badge key={tag} variant="secondary" className="gap-1 pr-1">
              {tag}
              <button
                type="button"
                onClick={() => handleRemoveTag(tag)}
                className="ml-1 hover:bg-secondary-foreground/10 rounded-full p-0.5"
              >
                <X className="h-3 w-3" />
              </button>
            </Badge>
          ))}
        </div>
        <Input
          id="tags"
          value={tagInput}
          onChange={(e) => setTagInput(e.target.value)}
          onKeyDown={handleAddTag}
          placeholder="태그 입력 후 Enter (최대 10개)"
          disabled={tags.length >= 10}
        />
        <p className="text-xs text-muted-foreground">
          종목명, 키워드 등을 태그로 추가하세요
        </p>
      </div>

      {/* Content */}
      <div className="space-y-2">
        <Label htmlFor="content">본문</Label>
        <Textarea
          id="content"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="분석 내용을 작성하세요..."
          className="min-h-80 resize-y"
          required
        />
      </div>

      {/* Attachments */}
      <div className="space-y-2">
        <Label>첨부파일</Label>
        <div
          className={cn(
            "border-2 border-dashed rounded-lg p-6 text-center transition-colors",
            dragOver
              ? "border-accent bg-accent/5"
              : "border-border hover:border-accent/50"
          )}
          onDragOver={(e) => {
            e.preventDefault()
            setDragOver(true)
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
        >
          <Upload className="mx-auto h-8 w-8 text-muted-foreground mb-2" />
          <p className="text-sm text-muted-foreground mb-2">
            파일을 드래그하거나 클릭하여 업로드
          </p>
          <p className="text-xs text-muted-foreground">
            JPG, PNG, GIF, PDF (최대 10MB)
          </p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept=".jpg,.jpeg,.png,.gif,.pdf"
            onChange={(e) => handleFileSelect(e.target.files)}
            className="hidden"
          />
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="mt-3"
            onClick={() => fileInputRef.current?.click()}
          >
            파일 선택
          </Button>
        </div>

        {/* Attachment List */}
        {attachments.length > 0 && (
          <div className="space-y-2 mt-4">
            {attachments.map((attachment) => (
              <div
                key={attachment.id}
                className="flex items-center gap-3 p-3 bg-secondary rounded-lg"
              >
                <div className="flex h-10 w-10 items-center justify-center rounded bg-muted">
                  {attachment.type === "pdf" ? (
                    <FileText className="h-5 w-5 text-muted-foreground" />
                  ) : (
                    <ImageIcon className="h-5 w-5 text-muted-foreground" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">
                    {attachment.name}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {attachment.size}
                  </p>
                  {attachment.uploading && (
                    <div className="mt-1 h-1 bg-muted rounded-full overflow-hidden">
                      <div
                        className="h-full bg-accent transition-all"
                        style={{ width: `${attachment.progress}%` }}
                      />
                    </div>
                  )}
                </div>
                {attachment.uploading ? (
                  <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                ) : (
                  <button
                    type="button"
                    onClick={() => handleRemoveAttachment(attachment.id)}
                    className="text-muted-foreground hover:text-foreground"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Submit */}
      <div className="flex items-center justify-end gap-3 pt-4 border-t border-border">
        <Button type="button" variant="outline">
          취소
        </Button>
        <Button
          type="submit"
          disabled={
            isSubmitting ||
            !title.trim() ||
            !content.trim() ||
            attachments.some((a) => a.uploading)
          }
        >
          {isSubmitting ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              등록 중...
            </>
          ) : (
            "게시하기"
          )}
        </Button>
      </div>
    </form>
  )
}
