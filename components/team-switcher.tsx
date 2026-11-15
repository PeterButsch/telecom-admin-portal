"use client"

import Link from "next/link"

import { Logo, Wordmark } from "@/components/logo"
import { SidebarMenu, SidebarMenuItem } from "@/components/ui/sidebar"

/** The Singtel mark and wordmark at the top of the sidebar, linking to the dashboard. */
export function TeamSwitcher({ product }: { product: string }) {
  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <Link
          href="/dashboard"
          aria-label={`Singtel ${product}, go to the dashboard`}
          className="hover:bg-sidebar-accent flex items-center gap-3 rounded-md px-2 py-3 transition-colors group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:p-0"
        >
          <Logo className="size-8" />
          <Wordmark product={product} className="flex-1 group-data-[collapsible=icon]:hidden" />
        </Link>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
