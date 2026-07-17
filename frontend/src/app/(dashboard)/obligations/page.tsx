"use client";

import { useState } from "react";
import { useApi } from "@/lib/hooks";
import type { Obligation, Page } from "@/types";
import { SEVERITY_COLORS, STATUS_COLORS } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { CheckCircle, Search } from "lucide-react";

export default function ObligationsPage() {
  const [page, setPage] = useState(0);
  const [severity, setSeverity] = useState("ALL");
  const [review, setReview] = useState("ALL");
  const [search, setSearch] = useState("");

  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", "20");
  if (severity !== "ALL") params.set("severity", severity);
  if (review !== "ALL") params.set("reviewStatus", review);
  if (search) params.set("search", search);

  const { data, loading } = useApi<Page<Obligation>>(
    `/api/v1/obligations?${params}`,
    [page, severity, review, search],
  );

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Obligations</h1>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <CheckCircle className="h-4 w-4" />
              All Obligations
              {data && <Badge variant="secondary">{data.totalElements}</Badge>}
            </CardTitle>
            <div className="flex flex-wrap gap-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search..."
                  className="pl-8 w-44"
                  value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                />
              </div>
              <Select value={severity} onValueChange={(v) => { if (v) { setSeverity(v); setPage(0); } }}>
                <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Severity</SelectItem>
                  <SelectItem value="LOW">Low</SelectItem>
                  <SelectItem value="MEDIUM">Medium</SelectItem>
                  <SelectItem value="HIGH">High</SelectItem>
                  <SelectItem value="CRITICAL">Critical</SelectItem>
                </SelectContent>
              </Select>
              <Select value={review} onValueChange={(v) => { if (v) { setReview(v); setPage(0); } }}>
                <SelectTrigger className="w-36"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Review</SelectItem>
                  <SelectItem value="PENDING_REVIEW">Pending</SelectItem>
                  <SelectItem value="APPROVED">Approved</SelectItem>
                  <SelectItem value="EDITED">Edited</SelectItem>
                  <SelectItem value="REJECTED">Rejected</SelectItem>
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
                      <TableHead>Title</TableHead>
                      <TableHead>Circular</TableHead>
                      <TableHead>Severity</TableHead>
                      <TableHead>Review</TableHead>
                      <TableHead>Applicability</TableHead>
                      <TableHead>Departments</TableHead>
                      <TableHead>Deadline</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data?.content.map((ob) => (
                      <TableRow key={ob.id}>
                        <TableCell className="text-sm font-medium max-w-[240px] truncate">
                          {ob.obligationTitle}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {ob.circularNumber || "—"}
                        </TableCell>
                        <TableCell>
                          <Badge className={SEVERITY_COLORS[ob.severity] || ""} variant="secondary">{ob.severity}</Badge>
                        </TableCell>
                        <TableCell>
                          <Badge className={STATUS_COLORS[ob.reviewStatus] || ""} variant="secondary">
                            {ob.reviewStatus.replace(/_/g, " ")}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge className={STATUS_COLORS[ob.applicabilityStatus] || ""} variant="secondary">
                            {ob.applicabilityStatus.replace(/_/g, " ")}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-sm">
                          {ob.impactedDepartments.slice(0, 2).join(", ")}
                          {ob.impactedDepartments.length > 2 && ` +${ob.impactedDepartments.length - 2}`}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {ob.deadline ? new Date(ob.deadline).toLocaleDateString() : "—"}
                        </TableCell>
                      </TableRow>
                    ))}
                    {data?.content.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={7} className="text-center text-muted-foreground py-8">No obligations found</TableCell>
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
