import { ExternalLink, FileText, Newspaper, Building2 } from "lucide-react"
import { cn } from "@/lib/utils"

interface SourceChipProps {
  type: "disclosure" | "news" | "report" | "link"
  title: string
  url?: string
  date?: string
  className?: string
}

export function SourceChip({
  type,
  title,
  url,
  date,
  className,
}: SourceChipProps) {
  const icons = {
    disclosure: Building2,
    news: Newspaper,
    report: FileText,
    link: ExternalLink,
  }

  const labels = {
    disclosure: "공시",
    news: "뉴스",
    report: "리포트",
    link: "링크",
  }

  const Icon = icons[type]
  const label = labels[type]

  const content = (
    <div
      className={cn(
        "flex items-center gap-2 px-3 py-2 rounded-lg bg-secondary/50 border border-border text-sm",
        url && "hover:bg-secondary hover:border-accent/30 transition-colors cursor-pointer",
        className
      )}
    >
      <div className="flex h-6 w-6 items-center justify-center rounded bg-muted">
        <Icon className="h-3.5 w-3.5 text-muted-foreground" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="text-xs text-muted-foreground">{label}</span>
          {url && <ExternalLink className="h-3 w-3 text-muted-foreground" />}
        </div>
        <p className="text-sm font-medium truncate">{title}</p>
        {date && <p className="text-xs text-muted-foreground">{date}</p>}
      </div>
    </div>
  )

  if (url) {
    return (
      <a href={url} target="_blank" rel="noopener noreferrer">
        {content}
      </a>
    )
  }

  return content
}
