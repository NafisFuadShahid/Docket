"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import { api } from "@/lib/api";
import type { Task, Evidence } from "@/types";
import { STATUS_COLORS, SEVERITY_COLORS } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { ArrowLeft, Send, Upload, FileText, User, Calendar, Clock } from "lucide-react";

export default function TaskDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const { data: task, loading, refetch } = useApi<Task>(`/api/v1/tasks/${id}`);
  const { data: evidence } = useApi<Evidence[]>(`/api/v1/evidence?taskId=${id}`);
  const [comment, setComment] = useState("");
  const [newStatus, setNewStatus] = useState("");
  const [sending, setSending] = useState(false);

  const addComment = async () => {
    if (!comment.trim()) return;
    setSending(true);
    try {
      await api.post(`/api/v1/tasks/${id}/comments`, { content: comment });
      setComment("");
      refetch();
    } finally {
      setSending(false);
    }
  };

  const updateStatus = async (status: string) => {
    await api.patch(`/api/v1/tasks/${id}`, { status });
    refetch();
  };

  if (loading || !task) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/tasks">
          <Button variant="ghost" size="icon" className="h-8 w-8">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div className="flex-1 min-w-0">
          <h1 className="text-xl font-bold">{task.title}</h1>
          <p className="text-sm text-muted-foreground">{task.taskType.replace(/_/g, " ")} · {task.department.replace(/_/g, " ")}</p>
        </div>
        <Badge className={STATUS_COLORS[task.status] || ""} variant="secondary">
          {task.status.replace(/_/g, " ")}
        </Badge>
        <Badge className={SEVERITY_COLORS[task.priority] || ""} variant="secondary">
          {task.priority}
        </Badge>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm">Description</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm">{task.description || "No description provided."}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm">Comments</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {task.comments && task.comments.length > 0 ? (
                task.comments.map((c) => (
                  <div key={c.id} className="flex gap-3">
                    <div className="h-7 w-7 rounded-full bg-muted flex items-center justify-center shrink-0">
                      <User className="h-3.5 w-3.5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium">{c.userName}</span>
                        <span className="text-xs text-muted-foreground">
                          {new Date(c.createdAt).toLocaleString()}
                        </span>
                      </div>
                      <p className="text-sm mt-0.5">{c.content}</p>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-sm text-muted-foreground">No comments yet</p>
              )}
              <Separator />
              <div className="flex gap-2">
                <Textarea
                  placeholder="Add a comment..."
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  rows={2}
                  className="flex-1"
                />
                <Button size="icon" className="h-10 w-10 shrink-0" onClick={addComment} disabled={sending || !comment.trim()}>
                  <Send className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3 flex-row items-center justify-between">
              <CardTitle className="text-sm">Evidence</CardTitle>
              <Button size="sm" variant="outline" className="gap-1">
                <Upload className="h-3 w-3" /> Upload
              </Button>
            </CardHeader>
            <CardContent>
              {evidence && evidence.length > 0 ? (
                <div className="space-y-2">
                  {evidence.map((e) => (
                    <div key={e.id} className="flex items-center gap-3 rounded-md border p-3">
                      <FileText className="h-4 w-4 text-muted-foreground" />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">{e.fileName}</p>
                        <p className="text-xs text-muted-foreground">
                          {(e.fileSize / 1024).toFixed(1)} KB · {e.uploaderName}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground text-center py-4">
                  {task.evidenceRequired ? "Evidence required — upload files" : "No evidence attached"}
                </p>
              )}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm">Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="flex items-center gap-2">
                <User className="h-4 w-4 text-muted-foreground" />
                <span className="text-muted-foreground">Owner:</span>
                <span className="font-medium">{task.ownerName || "Unassigned"}</span>
              </div>
              <div className="flex items-center gap-2">
                <Calendar className="h-4 w-4 text-muted-foreground" />
                <span className="text-muted-foreground">Due:</span>
                <span className="font-medium">{task.dueDate ? new Date(task.dueDate).toLocaleDateString() : "No deadline"}</span>
              </div>
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-muted-foreground" />
                <span className="text-muted-foreground">Created:</span>
                <span>{new Date(task.createdAt).toLocaleDateString()}</span>
              </div>
              <Separator />
              <div>
                <p className="text-muted-foreground mb-2">Update Status</p>
                <Select value={newStatus || task.status} onValueChange={(v) => { if (v) { setNewStatus(v); updateStatus(v); } }}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PENDING">Pending</SelectItem>
                    <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                    <SelectItem value="COMPLETED">Completed</SelectItem>
                    <SelectItem value="BLOCKED">Blocked</SelectItem>
                    <SelectItem value="CANCELLED">Cancelled</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {task.evidenceRequired && (
                <Badge variant="outline" className="border-amber-300 text-amber-700 dark:text-amber-400">
                  Evidence Required
                </Badge>
              )}
            </CardContent>
          </Card>

          {task.circularTitle && (
            <Card>
              <CardContent className="p-4">
                <p className="text-xs text-muted-foreground">From Circular</p>
                <p className="text-sm font-medium mt-0.5">{task.circularTitle}</p>
              </CardContent>
            </Card>
          )}
          {task.obligationTitle && (
            <Card>
              <CardContent className="p-4">
                <p className="text-xs text-muted-foreground">Obligation</p>
                <p className="text-sm font-medium mt-0.5">{task.obligationTitle}</p>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
