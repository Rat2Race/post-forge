import { FileText, Image, Download, X } from "lucide-react";
import { Button } from "./ui/button";

export interface AttachmentPillProps {
  id: string;
  fileName: string;
  fileType: string;
  fileSize?: number;
  url?: string;
  onRemove?: () => void;
  uploadProgress?: number;
}

export function AttachmentPill({ 
  fileName, 
  fileType, 
  fileSize, 
  url, 
  onRemove,
  uploadProgress 
}: AttachmentPillProps) {
  const isImage = ["jpg", "jpeg", "png", "gif"].includes(fileType.toLowerCase());
  const isPDF = fileType.toLowerCase() === "pdf";
  
  const formatFileSize = (bytes?: number) => {
    if (!bytes) return "";
    if (bytes < 1024) return `${bytes}B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
  };

  return (
    <div className="inline-flex items-center gap-2 px-3 py-2 bg-secondary rounded-lg border border-border text-sm group">
      <div className={`${isImage ? "text-positive" : isPDF ? "text-negative" : "text-muted-foreground"}`}>
        {isImage ? <Image className="h-4 w-4" /> : <FileText className="h-4 w-4" />}
      </div>
      
      <div className="flex-1 min-w-0">
        <div className="truncate font-medium">{fileName}</div>
        {fileSize && (
          <div className="text-xs text-muted-foreground">{formatFileSize(fileSize)}</div>
        )}
        {uploadProgress !== undefined && uploadProgress < 100 && (
          <div className="mt-1">
            <div className="h-1 bg-muted rounded-full overflow-hidden">
              <div 
                className="h-full bg-brass transition-all duration-300"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
          </div>
        )}
      </div>
      
      <div className="flex items-center gap-1">
        {url && (
          <Button
            variant="ghost"
            size="sm"
            className="h-6 w-6 p-0"
            asChild
          >
            <a href={url} download={fileName}>
              <Download className="h-3.5 w-3.5" />
            </a>
          </Button>
        )}
        {onRemove && (
          <Button
            variant="ghost"
            size="sm"
            className="h-6 w-6 p-0 opacity-0 group-hover:opacity-100 transition-opacity"
            onClick={onRemove}
          >
            <X className="h-3.5 w-3.5" />
          </Button>
        )}
      </div>
    </div>
  );
}
