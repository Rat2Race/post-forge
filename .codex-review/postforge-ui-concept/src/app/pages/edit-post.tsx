import { useState } from "react";
import { useParams, useNavigate } from "react-router";
import { Upload, X, Plus } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Textarea } from "../components/ui/textarea";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { AttachmentPill, AttachmentPillProps } from "../components/attachment-pill";

export function EditPostPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  // Mock existing post data
  const [formData, setFormData] = useState({
    title: "삼성전자 실적 발표 분석: HBM3E 수요 급증과 향후 전망",
    summary: "삼성전자의 최신 실적 발표를 심층 분석했습니다. HBM3E 메모리 수요 급증이 예상보다 빠르게 진행되고 있으며, AI 서버 시장 확대로 인한 수혜가 본격화될 것으로 전망됩니다.",
    content: "# 공시 분석\n\n삼성전자는 2026년 1분기 실적 발표에서 영업이익 6.5조원을 기록하며...",
    tags: ["삼성전자", "반도체", "HBM", "실적분석"],
  });
  const [tagInput, setTagInput] = useState("");
  const [attachments, setAttachments] = useState<AttachmentPillProps[]>([
    {
      id: "1",
      fileName: "삼성전자_1Q26_실적발표자료.pdf",
      fileType: "pdf",
      fileSize: 2450000,
      url: "#",
    },
  ]);

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
        alert(`${file.name}: 지원하지 않는 파일 형식입니다.`);
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

      // Simulate upload
      let progress = 0;
      const interval = setInterval(() => {
        progress += 10;
        setAttachments(prev =>
          prev.map(att =>
            att.id === newAttachment.id ? { ...att, uploadProgress: progress } : att
          )
        );

        if (progress >= 100) {
          clearInterval(interval);
          setAttachments(prev =>
            prev.map(att =>
              att.id === newAttachment.id ? { ...att, uploadProgress: 100, url: '#' } : att
            )
          );
        }
      }, 200);
    });
  };

  const handleRemoveAttachment = (attachmentId: string) => {
    setAttachments(prev => prev.filter(att => att.id !== attachmentId));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    navigate(`/posts/${id}`);
  };

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="serif-headline mb-2">글 수정</h1>
          <p className="text-muted-foreground">
            게시글 내용을 수정할 수 있습니다
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <Card className="p-6">
            <div className="space-y-4">
              <div>
                <Label htmlFor="title">제목</Label>
                <Input
                  id="title"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  required
                />
              </div>

              <div>
                <Label htmlFor="summary">요약</Label>
                <Textarea
                  id="summary"
                  value={formData.summary}
                  onChange={(e) => setFormData({ ...formData, summary: e.target.value })}
                  className="min-h-20"
                  required
                />
              </div>

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

          <Card className="p-6">
            <div>
              <Label htmlFor="content">본문</Label>
              <Textarea
                id="content"
                value={formData.content}
                onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                className="min-h-96 font-mono text-sm"
                required
              />
            </div>
          </Card>

          <Card className="p-6">
            <div className="space-y-4">
              <Label>첨부 파일</Label>
              
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

              <div className="border-2 border-dashed border-border rounded-lg p-6 text-center hover:border-brass/50 hover:bg-brass-light/30 transition-colors">
                <Upload className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
                <p className="text-sm mb-2">추가 파일 업로드</p>
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
                  size="sm"
                  onClick={() => document.getElementById('file-upload')?.click()}
                >
                  파일 선택
                </Button>
              </div>
            </div>
          </Card>

          <div className="flex gap-3 justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate(`/posts/${id}`)}
            >
              취소
            </Button>
            <Button type="submit">
              수정 완료
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
