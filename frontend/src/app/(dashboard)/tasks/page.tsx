"use client";

import { useState } from "react";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import type { Task, Page } from "@/types";
import { STATUS_COLORS, SEVERITY_COLORS } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { ListTodo, Search } from "lucide-react";

export default function TasksPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("ALL");
  const [priority, setPriority] = useState("ALL");
  const [search, setSearch] = useState("");

  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", "20");
  if (status !== "ALL") params.set("status", status);
  if (priority !== "ALL") params.set("priority", priority);
  if (search) params.set("search", search);

  const { data, loading } = useApi<Page<Task>>(
    `/api/v1/tasks?${params}`,
    [page, status, priority, search],
  );

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Tasks</h1>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <ListTodo className="h-4 w-4" />
              All Tasks
              {data && <Badge variant="secondary">{data.totalElements}</Badge>}
            </CardTitle>
            <div className="flex flex-wrap gap-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input placeholder="Search..." className="pl-8 w-44" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
              </div>
              <Select value={status} onValueChange={(v) => { if (v) { setStatus(v); setPage(0); } }}>
                <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Status</SelectItem>
                  <SelectItem value="PENDING">Pending</SelectItem>
                  <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                  <SelectItem value="COMPLETED">Completed</SelectItem>
                  <SelectItem value="BLOCKED">Blocked</SelectItem>
                  <SelectItem value="CANCELLED">Cancelled</SelectItem>
                </SelectContent>
              </Select>
              <Select value={priority} onValueChange={(v) => { if (v) { setPriority(v); setPage(0); } }}>
                <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Priority</SelectItem>
                  <SelectItem value="LOW">Low</SelectItem>
                  <SelectItem value="MEDIUM">Medium</SelectItem>
                  <SelectItem value="HIGH">High</SelectItem>
                  <SelectItem value="CRITICAL">Critical</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">{[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12" />)}</div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Task</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Department</TableHead>
                      <TableHead>Priority</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Due</TableHead>
                      <TableHead>Owner</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data?.content.map((t) => (
                      <TableRow key={t.id}>
                        <TableCell>
                          <Link href={`/tasks/${t.id}`} className="hover:underline text-sm font-medium">
                            {t.title}
                          </Link>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">{t.taskType.replace(/_/g, " ")}</TableCell>
                        <TableCell className="text-sm">{t.department.replace(/_/g, " ")}</TableCell>
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
                    {data?.content.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={7} className="text-center text-muted-foreground py-8">No tasks found</TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>
              {data && data.totalPages > 1 && (
                <div className="mt-4 flex items-center justify-between">
                  <p className="text-sm text-muted-foreground">Page {data.number + 1} of {data.totalPages}</p>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" disabled={data.number === 0} onClick={() => setPage(data.number - 1)}>Previous</Button>
                    <Button variant="outline" size="sm" disabled={data.number >= data.totalPages - 1} onClick={() => setPage(data.number + 1)}>Next</Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
