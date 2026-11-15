"use client"

import * as React from "react"
import { usePathname } from "next/navigation"

import { useAuth } from "@/components/auth-provider"
import { NavMain } from "@/components/nav-main"
import { NavUser } from "@/components/nav-user"
import { TeamSwitcher } from "@/components/team-switcher"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "@/components/ui/sidebar"
import { visibleNavItems } from "@/lib/navigation"

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const pathname = usePathname()
  const { can } = useAuth()
  const items = visibleNavItems(can, pathname)
  const operations = items.filter((item) => item.section === "operations")
  const administration = items.filter((item) => item.section === "administration")

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <TeamSwitcher product="Ops Console" />
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={operations} />
        {administration.length > 0 && <NavMain label="Administration" items={administration} />}
      </SidebarContent>
      <SidebarFooter>
        <NavUser />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
