export interface User {
  id: string;
  tenantId: string;
  email: string;
  fullName: string;
  role: string;
  department: string | null;
  isActive: boolean;
  createdAt: string;
  lastLogin: string | null;
}

export interface Circular {
  id: string;
  sourceId: string;
  sourceName: string;
  sourceType: string;
  circularNumber: string | null;
  title: string;
  titleBn: string | null;
  department: string | null;
  issuedDate: string | null;
  effectiveDate: string | null;
  sourceUrl: string | null;
  language: string;
  status: string;
  rawMetadata: Record<string, unknown>;
  firstSeenAt: string;
  lastSeenAt: string;
  createdAt: string;
  versionCount: number;
}

export interface CircularDetail {
  circular: Circular;
  versions: DocumentVersion[];
  extractedText: string | null;
  obligations: Obligation[];
}

export interface DocumentVersion {
  id: string;
  versionNumber: number;
  fileName: string;
  contentType: string;
  fileSize: number | null;
  sha256Hash: string;
  language: string;
  downloadedAt: string;
}

export interface Obligation {
  id: string;
  tenantId: string;
  circularId: string;
  circularTitle?: string;
  obligationTitle: string;
  obligationDetail: string;
  sourceQuote: string | null;
  sourcePage: number | null;
  regulator: string;
  circularNumber: string | null;
  sourceDepartment: string | null;
  affectedInstitutionTypes: string[];
  affectedBusinessLines: string[];
  impactedDepartments: string[];
  deadline: string | null;
  effectiveDate: string | null;
  requiredActions: string[];
  requiredEvidence: string[];
  severity: string;
  confidence: number;
  rationale: string | null;
  aiModelUsed: string | null;
  reviewStatus: string;
  reviewedBy: string | null;
  reviewedAt: string | null;
  reviewerNotes: string | null;
  applicabilityStatus: string;
  applicabilityReason: string | null;
  createdAt: string;
}

export interface Task {
  id: string;
  tenantId: string;
  obligationId: string | null;
  circularId: string | null;
  obligationTitle?: string;
  circularTitle?: string;
  title: string;
  description: string | null;
  taskType: string;
  ownerId: string | null;
  ownerName: string | null;
  department: string;
  dueDate: string | null;
  priority: string;
  status: string;
  evidenceRequired: boolean;
  approvalStatus: string;
  approvedBy: string | null;
  approvedAt: string | null;
  createdAt: string;
  updatedAt: string;
  comments?: TaskComment[];
  evidenceCount: number;
}

export interface TaskComment {
  id: string;
  userId: string;
  userName: string;
  content: string;
  createdAt: string;
}

export interface Evidence {
  id: string;
  tenantId: string;
  taskId: string | null;
  obligationId: string | null;
  fileName: string;
  contentType: string;
  fileSize: number;
  sha256Hash: string;
  evidenceType: string;
  uploadedBy: string;
  uploaderName: string;
  description: string | null;
  createdAt: string;
}

export interface Alert {
  id: string;
  tenantId: string;
  userId: string | null;
  alertType: string;
  title: string;
  message: string;
  severity: string;
  entityType: string | null;
  entityId: string | null;
  isRead: boolean;
  readAt: string | null;
  channel: string;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  tenantId: string;
  userId: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  oldValues: Record<string, unknown> | null;
  newValues: Record<string, unknown> | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface AuditPack {
  id: string;
  tenantId: string;
  circularId: string | null;
  generatedBy: string;
  title: string;
  packData: Record<string, unknown>;
  filePath: string | null;
  format: string;
  createdAt: string;
}

export interface InstitutionProfile {
  id: string;
  tenantId: string;
  institutionType: string;
  businessLines: string[];
  departments: string[];
  regulators: string[];
  createdAt: string;
  updatedAt: string;
}

export interface DashboardOverview {
  totalCirculars: number;
  pendingReview: number;
  overdueTasks: number;
  evidenceGaps: number;
  totalObligations: number;
  applicableObligations: number;
  totalTasks: number;
  completedTasks: number;
  unreadAlerts: number;
  complianceScore: number;
  obligationStats: ObligationStats;
  taskStats: TaskStats;
}

export interface ObligationStats {
  totalObligations: number;
  pendingReview: number;
  approved: number;
  rejected: number;
  applicable: number;
  notApplicable: number;
  highSeverity: number;
  criticalSeverity: number;
}

export interface TaskStats {
  totalTasks: number;
  pending: number;
  inProgress: number;
  completed: number;
  overdue: number;
  blocked: number;
}

export interface TimelineEntry {
  id: string;
  type: string;
  title: string;
  description: string;
  timestamp: string;
  entityType: string;
  entityId: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const DEPARTMENTS = [
  { slug: "credit_risk", label: "Credit Risk" },
  { slug: "trade_finance", label: "Trade Finance" },
  { slug: "aml_cft", label: "AML/CFT" },
  { slug: "ict_security", label: "ICT Security" },
  { slug: "treasury", label: "Treasury" },
  { slug: "operations", label: "Operations" },
  { slug: "legal", label: "Legal" },
  { slug: "branch_banking", label: "Branch Banking" },
  { slug: "compliance", label: "Internal Control & Compliance" },
] as const;

export const SEVERITY_COLORS: Record<string, string> = {
  LOW: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  MEDIUM: "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400",
  HIGH: "bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400",
  CRITICAL: "bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-400",
};

export const STATUS_COLORS: Record<string, string> = {
  DETECTED: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  DOWNLOADED: "bg-cyan-100 text-cyan-800 dark:bg-cyan-900/30 dark:text-cyan-400",
  TEXT_EXTRACTED: "bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400",
  AI_PROCESSED: "bg-violet-100 text-violet-800 dark:bg-violet-900/30 dark:text-violet-400",
  PENDING_REVIEW: "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400",
  APPROVED: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  ROUTED: "bg-teal-100 text-teal-800 dark:bg-teal-900/30 dark:text-teal-400",
  ARCHIVED: "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400",
  FAILED: "bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-400",
  PENDING: "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400",
  IN_PROGRESS: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  COMPLETED: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  BLOCKED: "bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-400",
  CANCELLED: "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400",
  EDITED: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  REJECTED: "bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-400",
  APPLICABLE: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  PARTIALLY_APPLICABLE: "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400",
  NOT_APPLICABLE: "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400",
  NEEDS_REVIEW: "bg-violet-100 text-violet-800 dark:bg-violet-900/30 dark:text-violet-400",
};
