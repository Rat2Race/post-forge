import { FileText, Image, Download } from "lucide-react"
import { cn } from "@/lib/utils"

interface AttachmentPillProps {
  fileName: string
  fileType: "image" | "pdf"
  fileSize?: string
  downloadUrl?: string
  className?: string
}

export function AttachmentPill({
  fileName,
  fileType,
  fileSize,
  downloadUrl,
  className,
}: AttachmentPillProps) {
  const Icon = fileType === "pdf" ? FileText : Image

  const content = (
    <div
      className={cn(
        "inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-secondary border border-border text-sm",
        downloadUrl && "hover:bg-secondary/80 hover:border-accent/30 transition-colors cursor-pointer",
        className
      )}
    >
      <Icon className="h-4 w-4 text-muted-foreground" />
      <span className="font-medium truncate max-w-32">{fileName}</span>
      {fileSize && (
        <span className="text-xs text-muted-foreground">({fileSize})</span>
      )}
      {downloadUrl && <Download className="h-3.5 w-3.5 text-muted-foreground" />}
    </div>
  )

  if (downloadUrl) {
    return (
      <a href={downloadUrl} download className="inline-block">
        {content}
      </a>
    )
  }

  return content
}
