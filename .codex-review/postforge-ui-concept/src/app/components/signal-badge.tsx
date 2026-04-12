import { TrendingUp, TrendingDown, Minus, AlertTriangle } from "lucide-react";
import { Badge } from "./ui/badge";

export interface SignalBadgeProps {
  type: "positive" | "negative" | "neutral" | "warning";
  label: string;
  className?: string;
}

export function SignalBadge({ type, label, className = "" }: SignalBadgeProps) {
  const config = {
    positive: {
      icon: TrendingUp,
      className: "bg-positive-light text-positive border-positive/30",
    },
    negative: {
      icon: TrendingDown,
      className: "bg-negative-light text-negative border-negative/30",
    },
    neutral: {
      icon: Minus,
      className: "bg-secondary text-muted-foreground border-border",
    },
    warning: {
      icon: AlertTriangle,
      className: "bg-brass-light text-brass border-brass/30",
    },
  };

  const { icon: Icon, className: typeClassName } = config[type];

  return (
    <Badge className={`${typeClassName} ${className} flex items-center gap-1.5`} variant="outline">
      <Icon className="h-3.5 w-3.5" />
      <span>{label}</span>
    </Badge>
  );
}
