"use client"

import { useMemo, useState } from "react"
import { Loader2, Plus, X } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { FilterSelect } from "@/components/filter-select"
import { PageShell } from "@/components/page-shell"
import { ResetPasswordDialog } from "@/components/reset-password-dialog"
import { SearchField } from "@/components/search-field"
import { StaffFormDialog } from "@/components/staff-form-dialog"
import { StaffTable } from "@/components/staff-table"
import {
  useStaffUsers,
  type StaffUser,
  type StaffUserCreate,
  type StaffUserUpdate,
} from "@/hooks/use-staff-users"
import { options } from "@/lib/labels"
import { STAFF_ROLES, type StaffRole } from "@/lib/permissions"

function matches(user: StaffUser, query: string) {
  const text = query.toLowerCase()
  return [user.display_name, user.username, user.email].some((v) =>
    v.toLowerCase().includes(text)
  )
}

export default function StaffPage() {
  const { user: currentUser } = useAuth()
  const { users, loading, error, createUser, updateUser, unlockUser, resetPassword } =
    useStaffUsers()
  const [search, setSearch] = useState("")
  const [role, setRole] = useState<StaffRole | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<StaffUser | null>(null)
  const [passwordUser, setPasswordUser] = useState<StaffUser | null>(null)
  const [unlockTarget, setUnlockTarget] = useState<StaffUser | null>(null)
  const currentUserId = currentUser?.id ?? null

  const shown = useMemo(() => {
    const query = search.trim()
    return users.filter((u) => (!role || u.role === role) && (!query || matches(u, query)))
  }, [users, search, role])

  const handleFormSubmit = async (data: StaffUserCreate | StaffUserUpdate) => {
    if (editingUser) {
      await updateUser(editingUser.id, data as StaffUserUpdate)
    } else {
      await createUser(data as StaffUserCreate)
    }
  }

  return (
    <PageShell title="Staff Accounts">
      <Card className="flex flex-1 flex-col overflow-hidden">
        <CardHeader className="shrink-0 space-y-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-headline-2">Staff Accounts</CardTitle>
            <div className="flex items-center gap-4">
              <span className="text-muted-foreground text-sm">
                {loading ? "Loading..." : `${shown.length} of ${users.length} accounts`}
              </span>
              <Button
                onClick={() => {
                  setEditingUser(null)
                  setFormOpen(true)
                }}
              >
                <Plus className="mr-2 h-4 w-4" />
                Add Staff
              </Button>
            </div>
          </div>
          <div className="flex flex-wrap items-end gap-4">
            <SearchField
              id="staff-search"
              label="Search"
              value={search}
              onChange={setSearch}
              placeholder="Name, username or email"
            />
            <FilterSelect
              id="role-filter"
              label="Role"
              value={role}
              options={options(STAFF_ROLES)}
              onChange={(v) => setRole(v as StaffRole | null)}
              className="w-[220px]"
            />
            {(search || role) && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSearch("")
                  setRole(null)
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
              <StaffTable
                users={shown}
                currentUserId={currentUserId}
                onEdit={(user) => {
                  setEditingUser(user)
                  setFormOpen(true)
                }}
                onResetPassword={setPasswordUser}
                onUnlock={setUnlockTarget}
              />
              {shown.length === 0 && (
                <div className="text-muted-foreground py-8 text-center">
                  No staff accounts match these filters.
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <StaffFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        user={editingUser}
        currentUserId={currentUserId}
        onSubmit={handleFormSubmit}
      />

      <ResetPasswordDialog
        user={passwordUser}
        onOpenChange={(open) => !open && setPasswordUser(null)}
        onSubmit={async (password) => {
          if (passwordUser) await resetPassword(passwordUser.id, password)
        }}
      />

      <ConfirmDialog
        open={unlockTarget !== null}
        onOpenChange={(open) => !open && setUnlockTarget(null)}
        title="Unlock Account"
        description={
          <>
            Let {unlockTarget?.display_name} ({unlockTarget?.username}) sign in again now instead of
            waiting for the lock to expire.
          </>
        }
        confirmLabel="Unlock"
        onConfirm={async () => {
          if (unlockTarget) await unlockUser(unlockTarget.id)
        }}
      />
    </PageShell>
  )
}
