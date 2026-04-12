import { ExternalLink } from "lucide-react";

export interface SourceChipProps {
  title: string;
  url: string;
  type?: "공시" | "뉴스" | "보고서" | "기타";
}

export function SourceChip({ title, url, type = "기타" }: SourceChipProps) {
  const typeColors = {
    "공시": "bg-positive-light text-positive border-positive/30",
    "뉴스": "bg-brass-light text-brass border-brass/30",
    "보고서": "bg-secondary text-foreground border-border",
    "기타": "bg-secondary text-muted-foreground border-border",
  };

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      className={`inline-flex items-center gap-2 px-3 py-2 rounded-md border text-sm hover:shadow-sm transition-all ${typeColors[type]} group`}
    >
      <span className="text-xs font-medium opacity-70">{type}</span>
      <span className="flex-1 truncate">{title}</span>
      <ExternalLink className="h-3.5 w-3.5 opacity-0 group-hover:opacity-100 transition-opacity" />
    </a>
  );
}
