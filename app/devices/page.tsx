"use client"

import { useMemo, useState } from "react"
import { Loader2, Plus, X } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { DeviceFormDialog } from "@/components/device-form-dialog"
import { DevicesTable } from "@/components/devices-table"
import { FilterSelect } from "@/components/filter-select"
import { PageShell } from "@/components/page-shell"
import { SearchField } from "@/components/search-field"
import {
  DEVICE_STATUSES,
  useDevices,
  type Device,
  type DeviceCreate,
  type DeviceStatus,
  type DeviceUpdate,
} from "@/hooks/use-devices"
import { options } from "@/lib/labels"

// Same rule as the backend's SearchTerms: only number-like queries search IMEIs and numbers,
// so "iPhone 16" does not match every IMEI containing "16".
const NUMBER_LIKE = /^[+0-9 ()-]+$/
const MIN_DIGITS = 3

function matches(device: Device, query: string) {
  const text = query.toLowerCase()
  const digits = NUMBER_LIKE.test(query) ? query.replace(/\D/g, "") : ""
  return (
    `${device.manufacturer} ${device.model}`.toLowerCase().includes(text) ||
    (device.customer_name ?? "").toLowerCase().includes(text) ||
    (digits.length >= MIN_DIGITS &&
      (device.imei.includes(digits) || (device.msisdn ?? "").includes(digits)))
  )
}

export default function DevicesPage() {
  const { can } = useAuth()
  const canWrite = can("DEVICE_WRITE")
  const canDelete = can("DEVICE_DELETE")
  const showLine = can("LINE_READ")
  const showCustomer = can("CUSTOMER_READ")
  const { devices, loading, error, createDevice, updateDevice, deleteDevice } = useDevices()
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState<DeviceStatus | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editingDevice, setEditingDevice] = useState<Device | null>(null)
  const [deviceToDelete, setDeviceToDelete] = useState<Device | null>(null)

  const shown = useMemo(() => {
    const query = search.trim()
    return devices.filter((d) => (!status || d.status === status) && (!query || matches(d, query)))
  }, [devices, search, status])

  const handleFormSubmit = async (data: DeviceCreate | DeviceUpdate) => {
    if (editingDevice) {
      await updateDevice(editingDevice.id, data as DeviceUpdate)
    } else {
      await createDevice(data as DeviceCreate)
    }
  }

  return (
    <PageShell title="Devices">
      <Card className="flex flex-1 flex-col overflow-hidden">
        <CardHeader className="shrink-0 space-y-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-headline-2">Device Inventory</CardTitle>
            <div className="flex items-center gap-4">
              <span className="text-muted-foreground text-sm">
                {loading
                  ? "Loading..."
                  : `${shown.length.toLocaleString()} of ${devices.length.toLocaleString()} devices`}
              </span>
              {canWrite && (
                <Button
                  onClick={() => {
                    setEditingDevice(null)
                    setFormOpen(true)
                  }}
                >
                  <Plus className="mr-2 h-4 w-4" />
                  Add Device
                </Button>
              )}
            </div>
          </div>
          <div className="flex flex-wrap items-end gap-4">
            <SearchField
              id="device-search"
              label="Search"
              value={search}
              onChange={setSearch}
              placeholder={
                showCustomer
                  ? "IMEI, model, mobile number or customer"
                  : showLine
                    ? "IMEI, model or mobile number"
                    : "IMEI or model"
              }
            />
            <FilterSelect
              id="status-filter"
              label="Status"
              value={status}
              options={options(DEVICE_STATUSES)}
              onChange={(v) => setStatus(v as DeviceStatus | null)}
            />
            {(search || status) && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSearch("")
                  setStatus(null)
                }}
              >
                <X className="mr-1 h-4 w-4" />
                Clear
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="flex-1 overflow-hidden">
          {loading ? (
            <div className="text-muted-foreground flex h-full items-center justify-center">
              <Loader2 className="mr-2 h-6 w-6 animate-spin" />
              Loading...
            </div>
          ) : error ? (
            <div role="alert" className="text-destructive py-8 text-center">
              {error}
            </div>
          ) : (
            <div className="h-full overflow-auto">
              <DevicesTable
                devices={shown}
                showLine={showLine}
                showCustomer={showCustomer}
                onEdit={
                  canWrite
                    ? (device) => {
                        setEditingDevice(device)
                        setFormOpen(true)
                      }
                    : undefined
                }
                onDelete={canDelete ? setDeviceToDelete : undefined}
              />
              {shown.length === 0 && (
                <div className="text-muted-foreground py-8 text-center">
                  No devices match these filters.
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <DeviceFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        device={editingDevice}
        onSubmit={handleFormSubmit}
      />

      <ConfirmDialog
        open={deviceToDelete !== null}
        onOpenChange={(open) => !open && setDeviceToDelete(null)}
        title="Delete Device"
        description={
          <>
            Delete {deviceToDelete?.manufacturer} {deviceToDelete?.model} ({deviceToDelete?.imei})?
            Devices assigned to a line must be released first.
          </>
        }
        onConfirm={async () => {
          if (deviceToDelete) await deleteDevice(deviceToDelete.id)
        }}
      />
    </PageShell>
  )
}
