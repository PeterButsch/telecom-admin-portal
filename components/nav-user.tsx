"use client"

import { useState } from "react"
import { useTheme } from "next-themes"
import { ChevronsUpDown, KeyRound, LogOut, Moon, Sun } from "lucide-react"

import { useAuth } from "@/components/auth-provider"
import { ChangePasswordDialog } from "@/components/change-password-dialog"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar"

function initials(name: string) {
  const parts = name.trim().split(/\s+/)
  const letters = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : name.slice(0, 2)
  return letters.toUpperCase()
}

function UserSummary({ name, detail }: { name: string; detail: string }) {
  return (
    <>
      <Avatar className="h-8 w-8 rounded-lg">
        <AvatarFallback className="bg-primary/10 text-primary rounded-lg">
          {initials(name)}
        </AvatarFallback>
      </Avatar>
      <div className="grid flex-1 text-left leading-tight">
        <span className="text-ui truncate">{name}</span>
        <span className="text-caption text-muted-foreground truncate">{detail}</span>
      </div>
    </>
  )
}

/** The signed-in staff member, with password, theme and sign-out actions. */
export function NavUser() {
  const { isMobile } = useSidebar()
  const { resolvedTheme, setTheme } = useTheme()
  const { user, signOut } = useAuth()
  const [passwordOpen, setPasswordOpen] = useState(false)

  if (!user) return null

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu modal={false}>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size="lg"
              aria-label={`Account menu for ${user.display_name}`}
              className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
            >
              <UserSummary name={user.display_name} detail={user.role_label} />
              <ChevronsUpDown className="ml-auto size-4" />
            </SidebarMenuButton>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
            side={isMobile ? "bottom" : "right"}
            align="end"
            sideOffset={4}
          >
            <DropdownMenuLabel className="p-0 font-normal">
              <div className="flex items-center gap-2 px-1 py-1.5 text-left">
                <UserSummary name={user.display_name} detail={user.email} />
              </div>
              <div className="text-caption text-muted-foreground px-1 pb-1.5">
                {user.role_label} · <span className="font-mono">{user.username}</span>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem onSelect={() => setPasswordOpen(true)}>
                <KeyRound />
                Change password
              </DropdownMenuItem>
              <DropdownMenuItem
                onSelect={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}
              >
                <Sun className="dark:hidden" />
                <Moon className="hidden dark:block" />
                Toggle theme
              </DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => void signOut()}>
              <LogOut />
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
        <ChangePasswordDialog open={passwordOpen} onOpenChange={setPasswordOpen} />
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
