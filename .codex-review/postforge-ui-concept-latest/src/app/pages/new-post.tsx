import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Upload, X, Plus, Sparkles, Lightbulb } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Textarea } from "../components/ui/textarea";
import { AttachmentPill, AttachmentPillProps } from "../components/attachment-pill";

export function NewPostPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    title: "", content: "", tags: [] as string[],
  });
  const [tagInput, setTagInput] = useState("");
  const [attachments, setAttachments] = useState<AttachmentPillProps[]>([]);
  const [isDragging, setIsDragging] = useState(false);

  const handleAddTag = () => {
    if (tagInput.trim() && !formData.tags.includes(tagInput.trim())) {
      setFormData({ ...formData, tags: [...formData.tags, tagInput.trim()] });
      setTagInput("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setFormData({ ...formData, tags: formData.tags.filter(tag => tag !== tagToRemove) });
  };

  const handleFileUpload = (files: FileList | null) => {
    if (!files) return;
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'application/pdf'];
    const maxSize = 10 * 1024 * 1024;

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

      let progress = 0;
      const interval = setInterval(() => {
        progress += 10;
        setAttachments(prev => prev.map(att => att.id === newAttachment.id ? { ...att, uploadProgress: progress } : att));
        if (progress >= 100) {
          clearInterval(interval);
          setAttachments(prev => prev.map(att => att.id === newAttachment.id ? { ...att, uploadProgress: 100, url: '#' } : att));
        }
      }, 200);
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    navigate('/posts/1');
  };

  return (
    <div className="px-5 lg:px-8 py-8 pb-16">
      <div className="max-w-3xl mx-auto">
        <div className="mb-6">
          <h1 className="serif-headline text-2xl mb-1">새 글 작성</h1>
          <p className="text-sm text-muted-foreground">투자 인사이트와 분석을 공유해주세요</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <Label htmlFor="title">제목</Label>
            <Input id="title" placeholder="분석 제목을 입력하세요" value={formData.title} onChange={(e) => setFormData({ ...formData, title: e.target.value })} required />
          </div>

          <div>
            <Label htmlFor="tags">태그</Label>
            <div className="flex gap-2 mb-2">
              <Input id="tags" placeholder="태그를 입력하고 Enter" value={tagInput} onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleAddTag(); } }} />
              <Button type="button" variant="outline" onClick={handleAddTag} size="sm"><Plus className="h-4 w-4" /></Button>
            </div>
            {formData.tags.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {formData.tags.map((tag, index) => (
                  <span key={index} className="inline-flex items-center gap-1 text-xs bg-secondary px-2 py-1 rounded">
                    {tag}
                    <button type="button" onClick={() => handleRemoveTag(tag)} className="hover:text-destructive transition-colors">
                      <X className="h-3 w-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div>
            <Label htmlFor="content">본문</Label>
            <Textarea id="content" placeholder="분석 내용을 작성해주세요 (마크다운 지원)" value={formData.content} onChange={(e) => setFormData({ ...formData, content: e.target.value })} className="min-h-80 font-mono text-sm" required />
            <p className="text-xs text-muted-foreground mt-1.5">마크다운 문법을 사용할 수 있습니다. **굵게**, *기울임*, # 제목</p>
          </div>

          <div>
            <Label>첨부 파일</Label>
            <div
              onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={(e) => { e.preventDefault(); setIsDragging(false); handleFileUpload(e.dataTransfer.files); }}
              className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
                isDragging ? 'border-brass/40 bg-brass-light/30' : 'border-border/60 hover:border-border'
              }`}
            >
              <Upload className="h-7 w-7 mx-auto mb-2.5 text-muted-foreground" />
              <p className="text-sm mb-1">파일을 드래그하거나 클릭하여 업로드</p>
              <p className="text-xs text-muted-foreground mb-3">JPG, PNG, GIF, PDF (최대 10MB)</p>
              <input type="file" id="file-upload" className="hidden" multiple accept=".jpg,.jpeg,.png,.gif,.pdf" onChange={(e) => handleFileUpload(e.target.files)} />
              <Button type="button" variant="outline" size="sm" onClick={() => document.getElementById('file-upload')?.click()}>파일 선택</Button>
            </div>

            {attachments.length > 0 && (
              <div className="mt-3">
                <p className="text-xs text-muted-foreground mb-2">업로드된 파일 ({attachments.length})</p>
                <div className="flex flex-wrap gap-2">
                  {attachments.map((attachment) => (
                    <AttachmentPill key={attachment.id} {...attachment} onRemove={() => setAttachments(prev => prev.filter(a => a.id !== attachment.id))} />
                  ))}
                </div>
              </div>
            )}
          </div>

          <div className="flex gap-3 justify-end pt-2 border-t border-border/40">
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>취소</Button>
            <Button type="submit">게시하기</Button>
          </div>
        </form>

        {/* Writing tips */}
        <div className="mt-12 pt-8 border-t border-border/40">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <div className="flex gap-3">
              <Lightbulb className="h-4 w-4 text-brass shrink-0 mt-0.5" />
              <div>
                <p className="text-sm text-foreground mb-1">좋은 분석의 요소</p>
                <p className="text-xs text-muted-foreground leading-relaxed">구체적인 데이터 근거, 명확한 투자 방향성, 리스크 요인 분석이 포함된 글이 높은 반응을 얻습니다.</p>
              </div>
            </div>
            <div className="flex gap-3">
              <Sparkles className="h-4 w-4 text-brass shrink-0 mt-0.5" />
              <div>
                <p className="text-sm text-foreground mb-1">AI 분석 활용</p>
                <p className="text-xs text-muted-foreground leading-relaxed">직접 작성이 어려우시면 <Link to="/ai/generate" className="text-brass hover:underline">AI 분석 생성</Link> 기능을 활용해보세요.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
