"use client";

import Link from "next/link";
import { useApi } from "@/lib/hooks";
import { DEPARTMENTS } from "@/types";
import type { DashboardOverview } from "@/types";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Building2 } from "lucide-react";

export default function DepartmentsPage() {
  const { data } = useApi<DashboardOverview>("/api/v1/dashboard/overview");

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Department Workspaces</h1>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {DEPARTMENTS.map((dept) => (
          <Link key={dept.slug} href={`/departments/${dept.slug}`}>
            <Card className="hover:border-emerald-300 dark:hover:border-emerald-800 transition-colors cursor-pointer h-full">
              <CardContent className="p-5">
                <div className="flex items-start gap-3">
                  <div className="rounded-lg bg-emerald-50 dark:bg-emerald-950/30 p-2">
                    <Building2 className="h-5 w-5 text-emerald-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-sm">{dept.label}</h3>
                    <p className="text-xs text-muted-foreground mt-0.5">{dept.slug.replace(/_/g, " ")}</p>
                  </div>
                </div>
                {data && (
                  <div className="mt-4 space-y-2">
                    <div className="flex justify-between text-xs">
                      <span className="text-muted-foreground">Tasks</span>
                      <span className="font-medium">View workspace</span>
                    </div>
                    <Progress value={65} className="h-1.5" />
                  </div>
                )}
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
