"use client";

import { useApi } from "@/lib/hooks";
import type { DashboardOverview, TimelineEntry } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import {
  FileText,
  ClipboardCheck,
  AlertTriangle,
  FolderArchive,
  CheckCircle,
  Clock,
  TrendingUp,
  Shield,
} from "lucide-react";

function ScoreGauge({ score }: { score: number }) {
  const color =
    score >= 80 ? "text-emerald-600" : score >= 60 ? "text-amber-500" : "text-rose-600";
  const bg =
    score >= 80 ? "stroke-emerald-600" : score >= 60 ? "stroke-amber-500" : "stroke-rose-600";
  const circumference = 2 * Math.PI * 54;
  const offset = circumference - (score / 100) * circumference;

  return (
    <div className="relative flex items-center justify-center">
      <svg className="h-36 w-36 -rotate-90" viewBox="0 0 120 120">
        <circle cx="60" cy="60" r="54" fill="none" strokeWidth="8" className="stroke-muted" />
        <circle
          cx="60"
          cy="60"
          r="54"
          fill="none"
          strokeWidth="8"
          strokeLinecap="round"
          className={bg}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{ transition: "stroke-dashoffset 1s ease" }}
        />
      </svg>
      <div className="absolute text-center">
        <span className={`text-3xl font-bold ${color}`}>{score}%</span>
        <p className="text-xs text-muted-foreground">Compliance</p>
      </div>
    </div>
  );
}

function StatCard({
  label,
  value,
  icon: Icon,
  trend,
  variant = "default",
}: {
  label: string;
  value: number;
  icon: React.ElementType;
  trend?: string;
  variant?: "default" | "warning" | "danger" | "success";
}) {
  const colors = {
    default: "text-blue-600 bg-blue-50 dark:bg-blue-950/30",
    warning: "text-amber-600 bg-amber-50 dark:bg-amber-950/30",
    danger: "text-rose-600 bg-rose-50 dark:bg-rose-950/30",
    success: "text-emerald-600 bg-emerald-50 dark:bg-emerald-950/30",
  };

  return (
    <Card>
      <CardContent className="flex items-center gap-4 p-4">
        <div className={`rounded-lg p-2.5 ${colors[variant]}`}>
          <Icon className="h-5 w-5" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm text-muted-foreground truncate">{label}</p>
          <p className="text-2xl font-bold">{value}</p>
        </div>
        {trend && (
          <Badge variant="secondary" className="text-xs">
            {trend}
          </Badge>
        )}
      </CardContent>
    </Card>
  );
}

function TimelineItem({ entry }: { entry: TimelineEntry }) {
  const ago = formatTimeAgo(entry.timestamp);
  return (
    <div className="flex gap-3 py-2">
      <div className="mt-1 h-2 w-2 rounded-full bg-emerald-500 shrink-0" />
      <div className="min-w-0 flex-1">
        <p className="text-sm truncate">{entry.title}</p>
        <p className="text-xs text-muted-foreground">{ago}</p>
      </div>
    </div>
  );
}

function formatTimeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

export default function DashboardPage() {
  const { data, loading } = useApi<DashboardOverview>("/api/v1/dashboard/overview");
  const { data: timeline } = useApi<TimelineEntry[]>("/api/v1/dashboard/timeline");

  if (loading || !data) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Dashboard</h1>

      <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Total Circulars" value={data.totalCirculars} icon={FileText} />
            <StatCard label="Pending Review" value={data.pendingReview} icon={ClipboardCheck} variant="warning" />
            <StatCard label="Overdue Tasks" value={data.overdueTasks} icon={AlertTriangle} variant="danger" />
            <StatCard label="Evidence Gaps" value={data.evidenceGaps} icon={FolderArchive} variant="warning" />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Obligations</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span>Approved</span>
                  <span className="font-medium">{data.obligationStats.approved}</span>
                </div>
                <Progress value={data.totalObligations > 0 ? (data.obligationStats.approved / data.totalObligations) * 100 : 0} className="h-2" />
                <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                  <span>Pending: {data.obligationStats.pendingReview}</span>
                  <span>Rejected: {data.obligationStats.rejected}</span>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Tasks</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span>Completed</span>
                  <span className="font-medium">{data.taskStats.completed}/{data.taskStats.totalTasks}</span>
                </div>
                <Progress value={data.taskStats.totalTasks > 0 ? (data.taskStats.completed / data.taskStats.totalTasks) * 100 : 0} className="h-2" />
                <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                  <span>In Progress: {data.taskStats.inProgress}</span>
                  <span>Blocked: {data.taskStats.blocked}</span>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Coverage</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between text-sm">
                  <span>Applicable</span>
                  <span className="font-medium">{data.applicableObligations}/{data.totalObligations}</span>
                </div>
                <Progress value={data.totalObligations > 0 ? (data.applicableObligations / data.totalObligations) * 100 : 0} className="h-2" />
                <div className="flex items-center gap-1 text-xs text-muted-foreground">
                  <TrendingUp className="h-3 w-3" />
                  <span>{data.unreadAlerts} unread alerts</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>

        <div className="space-y-4">
          <Card>
            <CardContent className="flex flex-col items-center py-6">
              <ScoreGauge score={data.complianceScore} />
              <div className="mt-3 flex items-center gap-1 text-xs text-muted-foreground">
                <Shield className="h-3 w-3" />
                <span>Compliance Health Score</span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Clock className="h-4 w-4" />
                Recent Activity
              </CardTitle>
            </CardHeader>
            <CardContent>
              {timeline && timeline.length > 0 ? (
                <div className="divide-y">
                  {timeline.slice(0, 8).map((e) => (
                    <TimelineItem key={e.id} entry={e} />
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground py-4 text-center">No recent activity</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardContent className="py-4">
              <div className="flex items-center gap-2">
                <CheckCircle className="h-4 w-4 text-emerald-600" />
                <span className="text-sm font-medium">Quick Stats</span>
              </div>
              <div className="mt-3 space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">High Severity</span>
                  <Badge variant="destructive" className="text-xs">{data.obligationStats.highSeverity}</Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Critical</span>
                  <Badge variant="destructive" className="text-xs">{data.obligationStats.criticalSeverity}</Badge>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
