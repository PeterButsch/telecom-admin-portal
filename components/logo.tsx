"use client"

import { cn } from "@/lib/utils"

/** The Singtel mark: a white "S" on a red tile. Brand red stays the same in light and dark mode. */
export function Logo({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      role="img"
      aria-label="Singtel logo"
      className={cn("shrink-0", className)}
    >
      <rect width="32" height="32" rx="8" fill="var(--brand)" />
      <text
        x="16"
        y="23"
        textAnchor="middle"
        fontSize="21"
        fontWeight="800"
        fontFamily="ui-sans-serif, system-ui, sans-serif"
        fill="#ffffff"
      >
        S
      </text>
    </svg>
  )
}

/** "Singtel" set in brand red, with an optional product name beneath it. */
export function Wordmark({ product, className }: { product?: string; className?: string }) {
  return (
    <div className={cn("grid leading-tight", className)}>
      <span className="text-brand text-xl font-extrabold tracking-tight">Singtel</span>
      {product && <span className="text-caption text-muted-foreground">{product}</span>}
    </div>
  )
}
