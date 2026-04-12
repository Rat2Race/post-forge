import { cn } from "@/lib/utils"
import { TrendingUp, TrendingDown, Minus } from "lucide-react"

interface SignalBadgeProps {
  signal: "positive" | "negative" | "neutral"
  label?: string
  size?: "sm" | "md" | "lg"
  showIcon?: boolean
  className?: string
}

export function SignalBadge({
  signal,
  label,
  size = "md",
  showIcon = true,
  className,
}: SignalBadgeProps) {
  const defaultLabels = {
    positive: "긍정",
    negative: "부정",
    neutral: "중립",
  }

  const icons = {
    positive: TrendingUp,
    negative: TrendingDown,
    neutral: Minus,
  }

  const Icon = icons[signal]
  const displayLabel = label || defaultLabels[signal]

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full font-medium",
        signal === "positive" && "bg-chart-1/15 text-chart-1 border border-chart-1/30",
        signal === "negative" && "bg-chart-2/15 text-chart-2 border border-chart-2/30",
        signal === "neutral" && "bg-muted text-muted-foreground border border-border",
        size === "sm" && "px-2 py-0.5 text-xs",
        size === "md" && "px-2.5 py-1 text-xs",
        size === "lg" && "px-3 py-1.5 text-sm",
        className
      )}
    >
      {showIcon && <Icon className={cn(
        size === "sm" && "h-3 w-3",
        size === "md" && "h-3.5 w-3.5",
        size === "lg" && "h-4 w-4"
      )} />}
      {displayLabel}
    </span>
  )
}
