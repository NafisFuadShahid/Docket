"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { useApi } from "@/lib/hooks";
import { api } from "@/lib/api";
import type { InstitutionProfile } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Settings, Building2, User, Lock } from "lucide-react";

function ProfileTab() {
  const { user } = useAuth();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base flex items-center gap-2">
          <User className="h-4 w-4" /> Your Profile
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label>Full Name</Label>
            <Input value={user?.fullName ?? ""} disabled />
          </div>
          <div>
            <Label>Email</Label>
            <Input value={user?.email ?? ""} disabled />
          </div>
          <div>
            <Label>Role</Label>
            <Input value={user?.role?.replace("ROLE_", "") ?? ""} disabled />
          </div>
          <div>
            <Label>Department</Label>
            <Input value={user?.department ?? "All"} disabled />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function PasswordTab() {
  const [current, setCurrent] = useState("");
  const [newPw, setNewPw] = useState("");
  const [confirm, setConfirm] = useState("");
  const [msg, setMsg] = useState("");

  const changePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPw !== confirm) {
      setMsg("Passwords do not match");
      return;
    }
    try {
      await api.post("/api/v1/auth/change-password", {
        currentPassword: current,
        newPassword: newPw,
      });
      setMsg("Password changed successfully");
      setCurrent("");
      setNewPw("");
      setConfirm("");
    } catch {
      setMsg("Failed to change password");
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base flex items-center gap-2">
          <Lock className="h-4 w-4" /> Change Password
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={changePassword} className="space-y-4 max-w-sm">
          <div>
            <Label>Current Password</Label>
            <Input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} required />
          </div>
          <div>
            <Label>New Password</Label>
            <Input type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)} required />
          </div>
          <div>
            <Label>Confirm New Password</Label>
            <Input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
          </div>
          {msg && <p className="text-sm text-muted-foreground">{msg}</p>}
          <Button type="submit">Change Password</Button>
        </form>
      </CardContent>
    </Card>
  );
}

function InstitutionTab() {
  const { data: profile, refetch } = useApi<InstitutionProfile>("/api/v1/institution/profile");
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<Partial<InstitutionProfile>>({});

  useEffect(() => {
    if (profile) setForm(profile);
  }, [profile]);

  const save = async () => {
    await api.put("/api/v1/institution/profile", form);
    setEditing(false);
    refetch();
  };

  if (!profile) return null;

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle className="text-base flex items-center gap-2">
          <Building2 className="h-4 w-4" /> Institution Profile
        </CardTitle>
        {!editing ? (
          <Button variant="outline" size="sm" onClick={() => setEditing(true)}>Edit</Button>
        ) : (
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => { setEditing(false); setForm(profile); }}>Cancel</Button>
            <Button size="sm" onClick={save} className="bg-emerald-600 hover:bg-emerald-700">Save</Button>
          </div>
        )}
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label>Institution Type</Label>
          <Input value={form.institutionType ?? ""} disabled={!editing} onChange={(e) => setForm({ ...form, institutionType: e.target.value })} />
        </div>
        <div>
          <Label>Business Lines</Label>
          <div className="flex flex-wrap gap-1.5 mt-1">
            {(form.businessLines ?? []).map((bl) => (
              <Badge key={bl} variant="secondary">{bl}</Badge>
            ))}
          </div>
        </div>
        <div>
          <Label>Departments</Label>
          <div className="flex flex-wrap gap-1.5 mt-1">
            {(form.departments ?? []).map((d) => (
              <Badge key={d} variant="secondary">{d.replace(/_/g, " ")}</Badge>
            ))}
          </div>
        </div>
        <div>
          <Label>Regulators</Label>
          <div className="flex flex-wrap gap-1.5 mt-1">
            {(form.regulators ?? []).map((r) => (
              <Badge key={r} variant="secondary">{r}</Badge>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export default function SettingsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Settings</h1>
      <Tabs defaultValue="profile">
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="password">Password</TabsTrigger>
          <TabsTrigger value="institution">Institution</TabsTrigger>
        </TabsList>
        <TabsContent value="profile" className="mt-4"><ProfileTab /></TabsContent>
        <TabsContent value="password" className="mt-4"><PasswordTab /></TabsContent>
        <TabsContent value="institution" className="mt-4"><InstitutionTab /></TabsContent>
      </Tabs>
    </div>
  );
}
