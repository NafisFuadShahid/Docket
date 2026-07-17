"use client";

import { use } from "react";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import { api } from "@/lib/api";
import type { CircularDetail } from "@/types";
import { STATUS_COLORS, SEVERITY_COLORS } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { ArrowLeft, Download, FileText, Bot } from "lucide-react";

export default function CircularDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const { data, loading, refetch } = useApi<CircularDetail>(`/api/v1/circulars/${id}`);

  const triggerExtraction = async () => {
    await api.post(`/api/v1/circulars/${id}/extract`);
    refetch();
  };

  if (loading || !data) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64" />
      </div>
    );
  }

  const { circular, versions, extractedText, obligations } = data;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/circulars">
          <Button variant="ghost" size="icon" className="h-8 w-8">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div className="flex-1 min-w-0">
          <h1 className="text-xl font-bold truncate">{circular.title}</h1>
          {circular.circularNumber && (
            <p className="text-sm text-muted-foreground">{circular.circularNumber}</p>
          )}
        </div>
        <Badge className={STATUS_COLORS[circular.status] || ""} variant="secondary">
          {circular.status.replace(/_/g, " ")}
        </Badge>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Source</p>
            <p className="text-sm font-medium">{circular.sourceName}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Department</p>
            <p className="text-sm font-medium">{circular.department || "—"}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Issued Date</p>
            <p className="text-sm font-medium">
              {circular.issuedDate ? new Date(circular.issuedDate).toLocaleDateString() : "—"}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <p className="text-xs text-muted-foreground">Language</p>
            <p className="text-sm font-medium">{circular.language}</p>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="obligations">
        <TabsList>
          <TabsTrigger value="obligations">
            Obligations ({obligations.length})
          </TabsTrigger>
          <TabsTrigger value="text">Extracted Text</TabsTrigger>
          <TabsTrigger value="versions">Versions ({versions.length})</TabsTrigger>
        </TabsList>

        <TabsContent value="obligations" className="mt-4">
          <Card>
            <CardHeader className="flex-row items-center justify-between pb-4">
              <CardTitle className="text-base">Extracted Obligations</CardTitle>
              <Button size="sm" variant="outline" className="gap-2" onClick={triggerExtraction}>
                <Bot className="h-4 w-4" />
                Re-extract
              </Button>
            </CardHeader>
            <CardContent>
              {obligations.length > 0 ? (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Obligation</TableHead>
                      <TableHead>Severity</TableHead>
                      <TableHead>Review</TableHead>
                      <TableHead>Departments</TableHead>
                      <TableHead>Deadline</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {obligations.map((ob) => (
                      <TableRow key={ob.id}>
                        <TableCell>
                          <Link href={`/obligations?id=${ob.id}`} className="hover:underline text-sm font-medium">
                            {ob.obligationTitle}
                          </Link>
                        </TableCell>
                        <TableCell>
                          <Badge className={SEVERITY_COLORS[ob.severity] || ""} variant="secondary">
                            {ob.severity}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge className={STATUS_COLORS[ob.reviewStatus] || ""} variant="secondary">
                            {ob.reviewStatus.replace(/_/g, " ")}
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
                  </TableBody>
                </Table>
              ) : (
                <p className="text-center text-muted-foreground py-8">
                  No obligations extracted yet
                </p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="text" className="mt-4">
          <Card>
            <CardContent className="p-6">
              {extractedText ? (
                <pre className="whitespace-pre-wrap text-sm leading-relaxed font-sans">
                  {extractedText}
                </pre>
              ) : (
                <p className="text-center text-muted-foreground py-8">
                  Text not yet extracted
                </p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="versions" className="mt-4">
          <Card>
            <CardContent className="p-6">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Version</TableHead>
                    <TableHead>File</TableHead>
                    <TableHead>Size</TableHead>
                    <TableHead>SHA-256</TableHead>
                    <TableHead>Downloaded</TableHead>
                    <TableHead className="w-10" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {versions.map((v) => (
                    <TableRow key={v.id}>
                      <TableCell className="font-medium">v{v.versionNumber}</TableCell>
                      <TableCell className="text-sm flex items-center gap-1">
                        <FileText className="h-3 w-3" />
                        {v.fileName}
                      </TableCell>
                      <TableCell className="text-sm">
                        {v.fileSize ? `${(v.fileSize / 1024).toFixed(1)} KB` : "—"}
                      </TableCell>
                      <TableCell className="text-xs font-mono text-muted-foreground">
                        {v.sha256Hash.slice(0, 16)}...
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {new Date(v.downloadedAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          <Download className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
