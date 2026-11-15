"use client"

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Pencil, Trash2 } from "lucide-react"
import { StatusBadge } from "@/components/status-badge"
import { formatDate, formatMsisdn } from "@/lib/format"
import type { Device } from "@/hooks/use-devices"

interface DevicesTableProps {
  devices: Device[]
  onEdit: (device: Device) => void
  onDelete: (device: Device) => void
}

export function DevicesTable({ devices, onEdit, onDelete }: DevicesTableProps) {
  return (
    <Table>
      <TableHeader className="bg-background sticky top-0 z-10">
        <TableRow>
          <TableHead>IMEI</TableHead>
          <TableHead>Device</TableHead>
          <TableHead>Mobile number</TableHead>
          <TableHead>Customer</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Assigned</TableHead>
          <TableHead className="w-[100px] text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {devices.map((device) => (
          <TableRow key={device.id}>
            <TableCell className="font-mono text-sm">{device.imei}</TableCell>
            <TableCell>
              <div className="font-medium">{device.model}</div>
              <div className="text-muted-foreground text-xs">{device.manufacturer}</div>
            </TableCell>
            <TableCell className="font-mono text-sm whitespace-nowrap">
              {formatMsisdn(device.msisdn)}
            </TableCell>
            <TableCell>{device.customer_name || "-"}</TableCell>
            <TableCell>
              <StatusBadge value={device.status} />
            </TableCell>
            <TableCell className="text-muted-foreground whitespace-nowrap">
              {formatDate(device.assigned_at)}
            </TableCell>
            <TableCell className="text-right">
              <div className="flex justify-end gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Edit device ${device.imei}`}
                  onClick={() => onEdit(device)}
                >
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Delete device ${device.imei}`}
                  onClick={() => onDelete(device)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
