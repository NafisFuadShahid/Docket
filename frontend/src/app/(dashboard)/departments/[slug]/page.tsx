"use client";

import { use } from "react";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import { DEPARTMENTS, STATUS_COLORS, SEVERITY_COLORS } from "@/types";
import type { Task, Obligation, Page } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { ArrowLeft, Building2 } from "lucide-react";

export default function DepartmentPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = use(params);
  const dept = DEPARTMENTS.find((d) => d.slug === slug);
  const { data: tasks, loading: tLoading } = useApi<Page<Task>>(
    `/api/v1/tasks?department=${slug}&size=50`,
  );
  const { data: obligations, loading: oLoading } = useApi<Page<Obligation>>(
    `/api/v1/obligations?department=${slug}&size=50`,
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/departments">
          <Button variant="ghost" size="icon" className="h-8 w-8">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <Building2 className="h-5 w-5 text-emerald-600" />
        <h1 className="text-2xl font-bold">{dept?.label || slug}</h1>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-2xl font-bold">{obligations?.totalElements ?? "—"}</p>
            <p className="text-sm text-muted-foreground">Obligations</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-2xl font-bold">{tasks?.totalElements ?? "—"}</p>
            <p className="text-sm text-muted-foreground">Tasks</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <p className="text-2xl font-bold">
              {tasks ? tasks.content.filter((t) => t.status === "COMPLETED").length : "—"}
            </p>
            <p className="text-sm text-muted-foreground">Completed</p>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="tasks">
        <TabsList>
          <TabsTrigger value="tasks">Tasks ({tasks?.totalElements ?? 0})</TabsTrigger>
          <TabsTrigger value="obligations">Obligations ({obligations?.totalElements ?? 0})</TabsTrigger>
        </TabsList>

        <TabsContent value="tasks" className="mt-4">
          <Card>
            <CardContent className="p-0">
              {tLoading ? (
                <div className="p-4 space-y-2">{[...Array(3)].map((_, i) => <Skeleton key={i} className="h-10" />)}</div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Task</TableHead>
                      <TableHead>Priority</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Due</TableHead>
                      <TableHead>Owner</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {tasks?.content.map((t) => (
                      <TableRow key={t.id}>
                        <TableCell>
                          <Link href={`/tasks/${t.id}`} className="hover:underline text-sm font-medium">
                            {t.title}
                          </Link>
                        </TableCell>
                        <TableCell>
                          <Badge className={SEVERITY_COLORS[t.priority] || ""} variant="secondary">{t.priority}</Badge>
                        </TableCell>
                        <TableCell>
                          <Badge className={STATUS_COLORS[t.status] || ""} variant="secondary">{t.status.replace(/_/g, " ")}</Badge>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {t.dueDate ? new Date(t.dueDate).toLocaleDateString() : "—"}
                        </TableCell>
                        <TableCell className="text-sm">{t.ownerName || "Unassigned"}</TableCell>
                      </TableRow>
                    ))}
                    {tasks?.content.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-muted-foreground py-8">No tasks</TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="obligations" className="mt-4">
          <Card>
            <CardContent className="p-0">
              {oLoading ? (
                <div className="p-4 space-y-2">{[...Array(3)].map((_, i) => <Skeleton key={i} className="h-10" />)}</div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Obligation</TableHead>
                      <TableHead>Severity</TableHead>
                      <TableHead>Review</TableHead>
                      <TableHead>Deadline</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {obligations?.content.map((ob) => (
                      <TableRow key={ob.id}>
                        <TableCell className="text-sm font-medium">{ob.obligationTitle}</TableCell>
                        <TableCell>
                          <Badge className={SEVERITY_COLORS[ob.severity] || ""} variant="secondary">{ob.severity}</Badge>
                        </TableCell>
                        <TableCell>
                          <Badge className={STATUS_COLORS[ob.reviewStatus] || ""} variant="secondary">{ob.reviewStatus.replace(/_/g, " ")}</Badge>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {ob.deadline ? new Date(ob.deadline).toLocaleDateString() : "—"}
                        </TableCell>
                      </TableRow>
                    ))}
                    {obligations?.content.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} className="text-center text-muted-foreground py-8">No obligations</TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
