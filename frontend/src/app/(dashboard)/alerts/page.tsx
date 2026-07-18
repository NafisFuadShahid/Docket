"use client";

import { useApi } from "@/lib/hooks";
import { api } from "@/lib/api";
import type { Alert, Page } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Bell, CheckCheck, AlertTriangle, Info, AlertOctagon } from "lucide-react";
import { cn } from "@/lib/utils";

const SEVERITY_ICON: Record<string, React.ElementType> = {
  LOW: Info,
  MEDIUM: Bell,
  HIGH: AlertTriangle,
  CRITICAL: AlertOctagon,
};

export default function AlertsPage() {
  const { data, loading, refetch } = useApi<Page<Alert>>("/api/v1/alerts?size=100");
  const alerts = data?.content;

  const markRead = async (id: string) => {
    await api.put(`/api/v1/alerts/${id}/read`);
    refetch();
  };

  const markAllRead = async () => {
    await api.put("/api/v1/alerts/read-all");
    refetch();
  };

  const unreadCount = alerts?.filter((a) => !a.isRead).length ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Alerts</h1>
        {unreadCount > 0 && (
          <Button variant="outline" size="sm" className="gap-2" onClick={markAllRead}>
            <CheckCheck className="h-4 w-4" /> Mark all read
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-base flex items-center gap-2">
            <Bell className="h-4 w-4" />
            Notifications
            {unreadCount > 0 && <Badge variant="destructive">{unreadCount} unread</Badge>}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">{[...Array(5)].map((_, i) => <Skeleton key={i} className="h-16" />)}</div>
          ) : alerts && alerts.length > 0 ? (
            <div className="space-y-1">
              {alerts.map((alert) => {
                const Icon = SEVERITY_ICON[alert.severity] || Bell;
                return (
                  <div
                    key={alert.id}
                    className={cn(
                      "flex items-start gap-3 rounded-lg p-3 transition-colors",
                      !alert.isRead && "bg-blue-50/50 dark:bg-blue-950/20",
                    )}
                  >
                    <div
                      className={cn(
                        "mt-0.5 rounded-full p-1.5",
                        alert.severity === "CRITICAL" && "bg-rose-100 text-rose-600 dark:bg-rose-950/30",
                        alert.severity === "HIGH" && "bg-orange-100 text-orange-600 dark:bg-orange-950/30",
                        alert.severity === "MEDIUM" && "bg-amber-100 text-amber-600 dark:bg-amber-950/30",
                        alert.severity === "LOW" && "bg-blue-100 text-blue-600 dark:bg-blue-950/30",
                      )}
                    >
                      <Icon className="h-3.5 w-3.5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <p className={cn("text-sm", !alert.isRead && "font-semibold")}>{alert.title}</p>
                        {!alert.isRead && <span className="h-2 w-2 rounded-full bg-blue-500" />}
                      </div>
                      <p className="text-sm text-muted-foreground mt-0.5">{alert.message}</p>
                      <p className="text-xs text-muted-foreground mt-1">
                        {new Date(alert.createdAt).toLocaleString()}
                      </p>
                    </div>
                    {!alert.isRead && (
                      <Button variant="ghost" size="sm" className="text-xs" onClick={() => markRead(alert.id)}>
                        Mark read
                      </Button>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="text-center text-muted-foreground py-8">No alerts</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
