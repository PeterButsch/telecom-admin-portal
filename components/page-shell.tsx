"use client"

import { AppSidebar } from "@/components/app-sidebar"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { Separator } from "@/components/ui/separator"
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { HealthStatus } from "@/components/health-status"
import { cn } from "@/lib/utils"

interface PageShellProps {
  title: string
  children: React.ReactNode
  /** Fit the page to the window so a list can scroll inside it, instead of scrolling the whole page. */
  fitToWindow?: boolean
}

/** Sidebar, breadcrumb header and content area shared by every portal page. */
export function PageShell({ title, children, fitToWindow = true }: PageShellProps) {
  const isDashboard = title === "Dashboard"
  return (
    <SidebarProvider className={cn(fitToWindow && "h-svh")}>
      <AppSidebar />
      <SidebarInset className={cn(fitToWindow && "overflow-hidden")}>
        <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-12">
          <div className="flex flex-1 items-center gap-2 px-4">
            <SidebarTrigger className="-ml-1" />
            <Separator orientation="vertical" className="mr-2 data-[orientation=vertical]:h-4" />
            <Breadcrumb>
              <BreadcrumbList>
                {!isDashboard && (
                  <>
                    <BreadcrumbItem className="hidden md:block">
                      <BreadcrumbLink href="/dashboard">Dashboard</BreadcrumbLink>
                    </BreadcrumbItem>
                    <BreadcrumbSeparator className="hidden md:block" />
                  </>
                )}
                <BreadcrumbItem>
                  <BreadcrumbPage>{title}</BreadcrumbPage>
                </BreadcrumbItem>
              </BreadcrumbList>
            </Breadcrumb>
            <div className="ml-auto">
              <HealthStatus />
            </div>
          </div>
        </header>
        <div
          className={cn("flex flex-1 flex-col gap-4 p-4 pt-0", fitToWindow && "overflow-hidden")}
        >
          {children}
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}
