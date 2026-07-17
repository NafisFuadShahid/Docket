"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import {
  LayoutDashboard,
  FileText,
  ClipboardCheck,
  ListTodo,
  Building2,
  FolderArchive,
  Shield,
  Bot,
  Bell,
  Settings,
  ChevronLeft,
  ChevronRight,
  LogOut,
  CheckCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

const NAV_ITEMS = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/circulars", label: "Regulatory Inbox", icon: FileText },
  { href: "/reviews", label: "Review Queue", icon: ClipboardCheck },
  { href: "/obligations", label: "Obligations", icon: CheckCircle },
  { href: "/departments", label: "Departments", icon: Building2 },
  { href: "/tasks", label: "Tasks", icon: ListTodo },
  { href: "/evidence", label: "Evidence Vault", icon: FolderArchive },
  { href: "/audit-packs", label: "Audit Packs", icon: Shield },
  { href: "/assistant", label: "AI Assistant", icon: Bot },
  { href: "/alerts", label: "Alerts", icon: Bell },
  { href: "/settings", label: "Settings", icon: Settings },
];

export function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <TooltipProvider delay={0}>
      <aside
        className={cn(
          "flex h-screen flex-col border-r border-border bg-card transition-all duration-200",
          collapsed ? "w-16" : "w-60",
        )}
      >
        <div className={cn("flex h-14 items-center border-b border-border px-4", collapsed && "justify-center")}>
          {!collapsed && (
            <Link href="/" className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-600 text-white text-sm font-bold">
                C
              </div>
              <span className="font-semibold text-sm tracking-tight">ComplianceOS</span>
            </Link>
          )}
          {collapsed && (
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-600 text-white text-sm font-bold">
              C
            </div>
          )}
        </div>

        <ScrollArea className="flex-1 px-2 py-2">
          <nav className="flex flex-col gap-0.5">
            {NAV_ITEMS.map((item) => {
              const isActive =
                item.href === "/"
                  ? pathname === "/"
                  : pathname.startsWith(item.href);
              const Icon = item.icon;
              const link = (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors",
                    isActive
                      ? "bg-emerald-50 text-emerald-700 font-medium dark:bg-emerald-950/40 dark:text-emerald-400"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground",
                    collapsed && "justify-center px-2",
                  )}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  {!collapsed && <span>{item.label}</span>}
                </Link>
              );

              if (collapsed) {
                return (
                  <Tooltip key={item.href}>
                    <TooltipTrigger render={link}></TooltipTrigger>
                    <TooltipContent side="right">{item.label}</TooltipContent>
                  </Tooltip>
                );
              }
              return link;
            })}
          </nav>
        </ScrollArea>

        <Separator />
        <div className={cn("p-2", collapsed && "flex flex-col items-center")}>
          {!collapsed && user && (
            <div className="mb-2 rounded-md bg-muted px-3 py-2">
              <p className="text-xs font-medium truncate">{user.fullName}</p>
              <p className="text-xs text-muted-foreground truncate">{user.role.replace("ROLE_", "")}</p>
            </div>
          )}
          <div className={cn("flex items-center", collapsed ? "flex-col gap-1" : "gap-1")}>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={() => setCollapsed(!collapsed)}
            >
              {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
            </Button>
            {user && (
              <Tooltip>
                <TooltipTrigger
                  render={<Button variant="ghost" size="icon" className="h-8 w-8" onClick={logout}><LogOut className="h-4 w-4" /></Button>}
                />
                <TooltipContent side={collapsed ? "right" : "top"}>Sign out</TooltipContent>
              </Tooltip>
            )}
          </div>
        </div>
      </aside>
    </TooltipProvider>
  );
}
