import { useState } from "react";
import { useNavigate } from "react-router";
import { Upload, X, Plus } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Textarea } from "../components/ui/textarea";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { AttachmentPill, AttachmentPillProps } from "../components/attachment-pill";

export function NewPostPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    title: "",
    summary: "",
    content: "",
    tags: [] as string[],
  });
  const [tagInput, setTagInput] = useState("");
  const [attachments, setAttachments] = useState<AttachmentPillProps[]>([]);
  const [isDragging, setIsDragging] = useState(false);

  const handleAddTag = () => {
    if (tagInput.trim() && !formData.tags.includes(tagInput.trim())) {
      setFormData({
        ...formData,
        tags: [...formData.tags, tagInput.trim()],
      });
      setTagInput("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setFormData({
      ...formData,
      tags: formData.tags.filter(tag => tag !== tagToRemove),
    });
  };

  const handleFileUpload = (files: FileList | null) => {
    if (!files) return;

    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'application/pdf'];
    const maxSize = 10 * 1024 * 1024; // 10MB

    Array.from(files).forEach((file) => {
      if (!allowedTypes.includes(file.type)) {
        alert(`${file.name}: 지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, pdf만 가능)`);
        return;
      }

      if (file.size > maxSize) {
        alert(`${file.name}: 파일 크기가 10MB를 초과합니다.`);
        return;
      }

      const newAttachment: AttachmentPillProps = {
        id: Date.now().toString() + Math.random(),
        fileName: file.name,
        fileType: file.name.split('.').pop() || '',
        fileSize: file.size,
        uploadProgress: 0,
      };

      setAttachments(prev => [...prev, newAttachment]);

      // Simulate upload progress
      let progress = 0;
      const interval = setInterval(() => {
        progress += 10;
        setAttachments(prev =>
          prev.map(att =>
            att.id === newAttachment.id
              ? { ...att, uploadProgress: progress }
              : att
          )
        );

        if (progress >= 100) {
          clearInterval(interval);
          setAttachments(prev =>
            prev.map(att =>
              att.id === newAttachment.id
                ? { ...att, uploadProgress: 100, url: '#' }
                : att
            )
          );
        }
      }, 200);
    });
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    handleFileUpload(e.dataTransfer.files);
  };

  const handleRemoveAttachment = (id: string) => {
    setAttachments(prev => prev.filter(att => att.id !== id));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // In real app, this would submit to backend
    navigate('/posts/1');
  };

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="serif-headline mb-2">새 글 작성</h1>
          <p className="text-muted-foreground">
            투자 인사이트와 분석을 공유해주세요
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Title */}
          <Card className="p-6">
            <div className="space-y-4">
              <div>
                <Label htmlFor="title">제목</Label>
                <Input
                  id="title"
                  placeholder="분석 제목을 입력하세요"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  required
                />
              </div>

              <div>
                <Label htmlFor="summary">요약</Label>
                <Textarea
                  id="summary"
                  placeholder="핵심 내용을 간단히 요약해주세요 (2-3줄)"
                  value={formData.summary}
                  onChange={(e) => setFormData({ ...formData, summary: e.target.value })}
                  className="min-h-20"
                  required
                />
              </div>

              {/* Tags */}
              <div>
                <Label htmlFor="tags">태그</Label>
                <div className="flex gap-2 mb-3">
                  <Input
                    id="tags"
                    placeholder="태그를 입력하고 Enter를 누르세요"
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddTag();
                      }
                    }}
                  />
                  <Button type="button" onClick={handleAddTag}>
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
                {formData.tags.length > 0 && (
                  <div className="flex flex-wrap gap-2">
                    {formData.tags.map((tag, index) => (
                      <Badge key={index} variant="secondary" className="gap-1">
                        {tag}
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="ml-1 hover:text-foreground"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </Badge>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </Card>

          {/* Content */}
          <Card className="p-6">
            <div>
              <Label htmlFor="content">본문</Label>
              <Textarea
                id="content"
                placeholder="분석 내용을 작성해주세요 (마크다운 지원)"
                value={formData.content}
                onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                className="min-h-96 font-mono text-sm"
                required
              />
              <p className="text-xs text-muted-foreground mt-2">
                마크다운 문법을 사용할 수 있습니다. **굵게**, *기울임*, # 제목 등
              </p>
            </div>
          </Card>

          {/* Attachments */}
          <Card className="p-6">
            <div className="space-y-4">
              <Label>첨부 파일</Label>
              
              <div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                  isDragging
                    ? 'border-brass bg-brass-light'
                    : 'border-border hover:border-brass/50 hover:bg-brass-light/30'
                }`}
              >
                <Upload className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                <p className="text-sm mb-2">
                  파일을 드래그하거나 클릭하여 업로드
                </p>
                <p className="text-xs text-muted-foreground mb-4">
                  JPG, JPEG, PNG, GIF, PDF (최대 10MB)
                </p>
                <input
                  type="file"
                  id="file-upload"
                  className="hidden"
                  multiple
                  accept=".jpg,.jpeg,.png,.gif,.pdf"
                  onChange={(e) => handleFileUpload(e.target.files)}
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => document.getElementById('file-upload')?.click()}
                >
                  파일 선택
                </Button>
              </div>

              {attachments.length > 0 && (
                <div className="space-y-3">
                  <h4>업로드된 파일 ({attachments.length})</h4>
                  <div className="flex flex-wrap gap-3">
                    {attachments.map((attachment) => (
                      <AttachmentPill
                        key={attachment.id}
                        {...attachment}
                        onRemove={() => handleRemoveAttachment(attachment.id)}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>
          </Card>

          {/* Actions */}
          <div className="flex gap-3 justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate(-1)}
            >
              취소
            </Button>
            <Button type="submit">
              게시하기
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
