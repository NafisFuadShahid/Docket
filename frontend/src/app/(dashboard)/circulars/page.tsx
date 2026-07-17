"use client";

import { useState } from "react";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import { api } from "@/lib/api";
import type { Circular, Page } from "@/types";
import { STATUS_COLORS } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { FileText, RefreshCw, Search, ExternalLink } from "lucide-react";

export default function CircularsPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<string>("ALL");
  const [search, setSearch] = useState("");
  const path = `/api/v1/circulars?page=${page}&size=20${status !== "ALL" ? `&status=${status}` : ""}${search ? `&search=${search}` : ""}`;
  const { data, loading, refetch } = useApi<Page<Circular>>(path, [page, status, search]);

  const triggerCrawl = async (sourceId: string) => {
    await api.post(`/api/v1/circulars/crawl/${sourceId}`);
    refetch();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Regulatory Inbox</h1>
        <Button
          variant="outline"
          size="sm"
          onClick={() => triggerCrawl("all")}
          className="gap-2"
        >
          <RefreshCw className="h-4 w-4" />
          Crawl All Sources
        </Button>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <FileText className="h-4 w-4" />
              Circulars
              {data && <Badge variant="secondary">{data.totalElements}</Badge>}
            </CardTitle>
            <div className="flex gap-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search..."
                  className="pl-8 w-48"
                  value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                />
              </div>
              <Select value={status} onValueChange={(v) => { if (v) { setStatus(v); setPage(0); } }}>
                <SelectTrigger className="w-36">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Status</SelectItem>
                  <SelectItem value="DETECTED">Detected</SelectItem>
                  <SelectItem value="DOWNLOADED">Downloaded</SelectItem>
                  <SelectItem value="TEXT_EXTRACTED">Extracted</SelectItem>
                  <SelectItem value="AI_PROCESSED">Processed</SelectItem>
                  <SelectItem value="PENDING_REVIEW">Pending Review</SelectItem>
                  <SelectItem value="APPROVED">Approved</SelectItem>
                  <SelectItem value="ARCHIVED">Archived</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12" />)}
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Circular</TableHead>
                      <TableHead>Source</TableHead>
                      <TableHead>Department</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Issued</TableHead>
                      <TableHead className="w-10" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data?.content.map((c) => (
                      <TableRow key={c.id}>
                        <TableCell>
                          <Link href={`/circulars/${c.id}`} className="hover:underline font-medium text-sm">
                            {c.circularNumber && <span className="text-muted-foreground mr-1">{c.circularNumber}</span>}
                            {c.title}
                          </Link>
                        </TableCell>
                        <TableCell className="text-sm">{c.sourceName}</TableCell>
                        <TableCell className="text-sm">{c.department || "—"}</TableCell>
                        <TableCell>
                          <Badge className={STATUS_COLORS[c.status] || ""} variant="secondary">
                            {c.status.replace(/_/g, " ")}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {c.issuedDate ? new Date(c.issuedDate).toLocaleDateString() : "—"}
                        </TableCell>
                        <TableCell>
                          {c.sourceUrl && (
                            <a href={c.sourceUrl} target="_blank" rel="noopener noreferrer">
                              <ExternalLink className="h-4 w-4 text-muted-foreground hover:text-foreground" />
                            </a>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                    {data?.content.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                          No circulars found
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>
              {data && data.totalPages > 1 && (
                <div className="mt-4 flex items-center justify-between">
                  <p className="text-sm text-muted-foreground">
                    Page {data.number + 1} of {data.totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" disabled={data.number === 0} onClick={() => setPage(data.number - 1)}>
                      Previous
                    </Button>
                    <Button variant="outline" size="sm" disabled={data.number >= data.totalPages - 1} onClick={() => setPage(data.number + 1)}>
                      Next
                    </Button>
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
