"use client";

import { useState } from "react";
import { useApi } from "@/lib/hooks";
import { api } from "@/lib/api";
import type { Obligation, Page } from "@/types";
import { SEVERITY_COLORS, STATUS_COLORS } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { CheckCircle, XCircle, Edit, ClipboardCheck, Quote } from "lucide-react";

export default function ReviewsPage() {
  const [page, setPage] = useState(0);
  const { data, loading, refetch } = useApi<Page<Obligation>>(
    `/api/v1/obligations?reviewStatus=PENDING_REVIEW&page=${page}&size=20`,
    [page],
  );
  const [selected, setSelected] = useState<Obligation | null>(null);
  const [notes, setNotes] = useState("");
  const [acting, setActing] = useState(false);

  const review = async (action: "approve" | "reject") => {
    if (!selected) return;
    setActing(true);
    try {
      await api.put(`/api/v1/obligations/${selected.id}/review`, {
        action,
        reviewerNotes: notes || undefined,
      });
      setSelected(null);
      setNotes("");
      refetch();
    } finally {
      setActing(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Review Queue</h1>
        {data && (
          <Badge variant="secondary" className="text-sm">
            {data.totalElements} pending
          </Badge>
        )}
      </div>

      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-base flex items-center gap-2">
            <ClipboardCheck className="h-4 w-4" />
            Obligations Pending Review
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12" />)}
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Obligation</TableHead>
                    <TableHead>Circular</TableHead>
                    <TableHead>Severity</TableHead>
                    <TableHead>Confidence</TableHead>
                    <TableHead>Departments</TableHead>
                    <TableHead className="w-28">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content.map((ob) => (
                    <TableRow key={ob.id}>
                      <TableCell>
                        <button
                          className="text-left hover:underline text-sm font-medium"
                          onClick={() => { setSelected(ob); setNotes(""); }}
                        >
                          {ob.obligationTitle}
                        </button>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {ob.circularNumber || ob.circularTitle || "—"}
                      </TableCell>
                      <TableCell>
                        <Badge className={SEVERITY_COLORS[ob.severity] || ""} variant="secondary">
                          {ob.severity}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm">{(ob.confidence * 100).toFixed(0)}%</TableCell>
                      <TableCell className="text-sm">
                        {ob.impactedDepartments.slice(0, 2).join(", ")}
                        {ob.impactedDepartments.length > 2 && ` +${ob.impactedDepartments.length - 2}`}
                      </TableCell>
                      <TableCell>
                        <div className="flex gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-emerald-600"
                            title="Quick approve"
                            onClick={async () => { await api.put(`/api/v1/obligations/${ob.id}/review`, { action: "approve" }); refetch(); }}
                          >
                            <CheckCircle className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-muted-foreground"
                            title="Review details"
                            onClick={() => { setSelected(ob); setNotes(""); }}
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                  {data?.content.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                        No obligations pending review
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
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

      <Dialog open={!!selected} onOpenChange={() => setSelected(null)}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          {selected && (
            <>
              <DialogHeader>
                <DialogTitle>{selected.obligationTitle}</DialogTitle>
              </DialogHeader>
              <div className="space-y-4">
                <div>
                  <p className="text-sm font-medium mb-1">Detail</p>
                  <p className="text-sm text-muted-foreground">{selected.obligationDetail}</p>
                </div>
                {selected.sourceQuote && (
                  <div className="border-l-2 border-emerald-500 pl-3">
                    <p className="text-xs text-muted-foreground flex items-center gap-1 mb-1">
                      <Quote className="h-3 w-3" /> Source Quote
                    </p>
                    <p className="text-sm italic">{selected.sourceQuote}</p>
                  </div>
                )}
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <span className="text-muted-foreground">Severity: </span>
                    <Badge className={SEVERITY_COLORS[selected.severity] || ""} variant="secondary">{selected.severity}</Badge>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Confidence: </span>
                    <span className="font-medium">{(selected.confidence * 100).toFixed(0)}%</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Regulator: </span>
                    <span>{selected.regulator}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Deadline: </span>
                    <span>{selected.deadline ? new Date(selected.deadline).toLocaleDateString() : "None"}</span>
                  </div>
                </div>
                {selected.requiredActions.length > 0 && (
                  <div>
                    <p className="text-sm font-medium mb-1">Required Actions</p>
                    <ul className="list-disc list-inside text-sm text-muted-foreground space-y-0.5">
                      {selected.requiredActions.map((a, i) => <li key={i}>{a}</li>)}
                    </ul>
                  </div>
                )}
                <div>
                  <p className="text-sm font-medium mb-1">Reviewer Notes</p>
                  <Textarea
                    placeholder="Add notes for this review decision..."
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    rows={3}
                  />
                </div>
                <div className="flex gap-2 justify-end">
                  <Button variant="outline" onClick={() => setSelected(null)}>Cancel</Button>
                  <Button
                    variant="destructive"
                    onClick={() => review("reject")}
                    disabled={acting}
                    className="gap-1"
                  >
                    <XCircle className="h-4 w-4" /> Reject
                  </Button>
                  <Button
                    onClick={() => review("approve")}
                    disabled={acting}
                    className="gap-1 bg-emerald-600 hover:bg-emerald-700"
                  >
                    <CheckCircle className="h-4 w-4" /> Approve
                  </Button>
                </div>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
