"use client";

import { useState } from "react";
import { useApi } from "@/lib/hooks";
import { api } from "@/lib/api";
import type { AuditPack, Circular } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { Shield, Download, Plus, FileText } from "lucide-react";

export default function AuditPacksPage() {
  const { data: packs, loading, refetch } = useApi<AuditPack[]>("/api/v1/audit-packs");
  const { data: circulars } = useApi<{ content: Circular[] }>("/api/v1/circulars?size=100");
  const [showGenerate, setShowGenerate] = useState(false);
  const [selectedCircular, setSelectedCircular] = useState("");
  const [generating, setGenerating] = useState(false);

  const generate = async () => {
    if (!selectedCircular) return;
    setGenerating(true);
    try {
      await api.post(`/api/v1/audit-packs/generate`, { circularId: selectedCircular });
      setShowGenerate(false);
      setSelectedCircular("");
      refetch();
    } finally {
      setGenerating(false);
    }
  };

  const download = async (id: string, title: string) => {
    const blob = await api.download(`/api/v1/audit-packs/${id}/download`);
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `${title}.html`;
    a.click();
    URL.revokeObjectURL(a.href);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Audit Packs</h1>
        <Button size="sm" className="gap-2 bg-emerald-600 hover:bg-emerald-700" onClick={() => setShowGenerate(true)}>
          <Plus className="h-4 w-4" /> Generate Pack
        </Button>
      </div>

      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-base flex items-center gap-2">
            <Shield className="h-4 w-4" />
            Generated Audit Packs
            {packs && <Badge variant="secondary">{packs.length}</Badge>}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">{[...Array(3)].map((_, i) => <Skeleton key={i} className="h-12" />)}</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Title</TableHead>
                  <TableHead>Format</TableHead>
                  <TableHead>Generated</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {packs?.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="text-sm font-medium flex items-center gap-2">
                      <FileText className="h-4 w-4 text-muted-foreground" />
                      {p.title}
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{p.format}</Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(p.createdAt).toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => download(p.id, p.title)}>
                        <Download className="h-3.5 w-3.5" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
                {packs?.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground py-8">
                      No audit packs generated yet
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={showGenerate} onOpenChange={setShowGenerate}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Generate Audit Pack</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <p className="text-sm text-muted-foreground mb-2">Select a circular to generate an audit pack for:</p>
              <Select value={selectedCircular} onValueChange={(v) => { if (v) setSelectedCircular(v); }}>
                <SelectTrigger><SelectValue placeholder="Select circular..." /></SelectTrigger>
                <SelectContent>
                  {circulars?.content.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.circularNumber || c.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setShowGenerate(false)}>Cancel</Button>
              <Button onClick={generate} disabled={!selectedCircular || generating} className="bg-emerald-600 hover:bg-emerald-700">
                {generating ? "Generating..." : "Generate"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
