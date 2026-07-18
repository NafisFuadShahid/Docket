"use client";

import { useRef, useState } from "react";
import { useApi } from "@/lib/hooks";
import { api } from "@/lib/api";
import type { Evidence, Page } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { FolderArchive, Upload, Download, Trash2, Search, FileText } from "lucide-react";

export default function EvidencePage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);
  const { data, loading, refetch } = useApi<Page<Evidence>>(
    `/api/v1/evidence?page=${page}&size=20${search ? `&search=${search}` : ""}`,
    [page, search],
  );

  const upload = async (files: FileList) => {
    for (const file of Array.from(files)) {
      const fd = new FormData();
      fd.append("file", file);
      fd.append("evidenceType", "DOCUMENT");
      await api.upload("/api/v1/evidence/upload", fd);
    }
    refetch();
  };

  const deleteEvidence = async (id: string) => {
    await api.delete(`/api/v1/evidence/${id}`);
    refetch();
  };

  const downloadEvidence = async (id: string, fileName: string) => {
    const blob = await api.download(`/api/v1/evidence/${id}/download`);
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = fileName;
    a.click();
    URL.revokeObjectURL(a.href);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Evidence Vault</h1>
        <div>
          <input
            ref={fileRef}
            type="file"
            multiple
            className="hidden"
            onChange={(e) => e.target.files && upload(e.target.files)}
          />
          <Button size="sm" className="gap-2 bg-emerald-600 hover:bg-emerald-700" onClick={() => fileRef.current?.click()}>
            <Upload className="h-4 w-4" /> Upload Evidence
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <FolderArchive className="h-4 w-4" />
              Evidence Files
              {data && <Badge variant="secondary">{data.totalElements}</Badge>}
            </CardTitle>
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input placeholder="Search..." className="pl-8 w-48" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
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
                      <TableHead>File</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Size</TableHead>
                      <TableHead>SHA-256</TableHead>
                      <TableHead>Uploaded By</TableHead>
                      <TableHead>Date</TableHead>
                      <TableHead className="w-20" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data?.content.map((e) => (
                      <TableRow key={e.id}>
                        <TableCell className="text-sm font-medium flex items-center gap-2">
                          <FileText className="h-4 w-4 text-muted-foreground shrink-0" />
                          <span className="truncate max-w-[200px]">{e.fileName}</span>
                        </TableCell>
                        <TableCell>
                          <Badge variant="secondary">{e.evidenceType}</Badge>
                        </TableCell>
                        <TableCell className="text-sm">{(e.fileSize / 1024).toFixed(1)} KB</TableCell>
                        <TableCell className="text-xs font-mono text-muted-foreground">{e.sha256Hash.slice(0, 16)}...</TableCell>
                        <TableCell className="text-sm">{e.uploaderName}</TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {new Date(e.createdAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell>
                          <div className="flex gap-1">
                            <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => downloadEvidence(e.id, e.fileName)}>
                              <Download className="h-3.5 w-3.5" />
                            </Button>
                            <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => deleteEvidence(e.id)}>
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                    {data?.content.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={7} className="text-center text-muted-foreground py-8">No evidence files</TableCell>
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
