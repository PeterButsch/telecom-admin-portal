"use client"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import type { BrandFigures, SegmentFigures } from "@/hooks/use-dashboard-overview"
import { BRAND_COLOURS, SEGMENT_COLOURS, share } from "@/lib/dashboard"
import { formatCount, formatMoney, formatPercent } from "@/lib/format"
import { label } from "@/lib/labels"

function Swatch({ colour }: { colour: string }) {
  return (
    <span
      aria-hidden
      className="mr-2 inline-block size-2.5 rounded-[2px]"
      style={{ backgroundColor: colour }}
    />
  )
}

/** Lines, revenue and ARPU for each brand, with each brand's share of revenue. */
export function BrandTable({ brands }: { brands: BrandFigures[] }) {
  const total = brands.reduce((sum, row) => sum + row.monthly_revenue, 0)
  return (
    <Table aria-label="Revenue and ARPU by brand">
      <TableHeader>
        <TableRow>
          <TableHead>Brand</TableHead>
          <TableHead className="text-right">Active lines</TableHead>
          <TableHead className="text-right">MRR</TableHead>
          <TableHead className="text-right">ARPU</TableHead>
          <TableHead className="text-right">Share</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {brands.map((row) => (
          <TableRow key={row.brand}>
            <TableCell>
              <Swatch colour={BRAND_COLOURS[row.brand]} />
              {label(row.brand)}
            </TableCell>
            <TableCell className="text-right">{formatCount(row.active_lines)}</TableCell>
            <TableCell className="text-right">{formatMoney(row.monthly_revenue)}</TableCell>
            <TableCell className="text-right">{formatMoney(row.arpu)}</TableCell>
            <TableCell className="text-right">
              {formatPercent(share(row.monthly_revenue, total))}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

const SEGMENT_NAMES = { CONSUMER: "Consumer", ENTERPRISE: "Enterprise" } as const

/** Consumer (individual) against enterprise (business) customers, with a bar of revenue share. */
export function SegmentComparison({ segments }: { segments: SegmentFigures[] }) {
  const total = segments.reduce((sum, row) => sum + row.monthly_revenue, 0)
  const shares = segments.map((row) => ({ ...row, share: share(row.monthly_revenue, total) }))
  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <div className="text-body-small text-muted-foreground flex justify-between">
          {shares.map((row) => (
            <span key={row.segment}>
              <Swatch colour={SEGMENT_COLOURS[row.segment]} />
              {SEGMENT_NAMES[row.segment]} {formatPercent(row.share)} of MRR
            </span>
          ))}
        </div>
        <div aria-hidden className="bg-muted flex h-3 w-full overflow-hidden rounded-full">
          {shares.map((row) => (
            <div
              key={row.segment}
              style={{
                width: `${row.share ?? 0}%`,
                backgroundColor: SEGMENT_COLOURS[row.segment],
              }}
            />
          ))}
        </div>
      </div>
      <Table aria-label="Consumer and enterprise">
        <TableHeader>
          <TableRow>
            <TableHead>Segment</TableHead>
            <TableHead className="text-right">Customers</TableHead>
            <TableHead className="text-right">Active lines</TableHead>
            <TableHead className="text-right">MRR</TableHead>
            <TableHead className="text-right">ARPU</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {segments.map((row) => (
            <TableRow key={row.segment}>
              <TableCell>
                <Swatch colour={SEGMENT_COLOURS[row.segment]} />
                {SEGMENT_NAMES[row.segment]}
              </TableCell>
              <TableCell className="text-right">{formatCount(row.active_customers)}</TableCell>
              <TableCell className="text-right">{formatCount(row.active_lines)}</TableCell>
              <TableCell className="text-right">{formatMoney(row.monthly_revenue)}</TableCell>
              <TableCell className="text-right">{formatMoney(row.arpu)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
