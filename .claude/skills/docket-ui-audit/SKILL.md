---
name: docket-ui-audit
description: Audit frontend pages for consistency, accessibility, and enterprise UX standards
---

# Docket UI Audit

Audit frontend pages against the platform's UX standards.

## Audit Checklist

### Per Page
1. **Loading state**: Shows skeleton or spinner while data loads
2. **Empty state**: Meaningful message when no data exists (not blank)
3. **Error state**: API errors displayed with retry option
4. **Responsive**: Usable on tablet (1024px) and mobile (375px)
5. **Dark mode**: All elements visible in both themes
6. **Navigation**: Breadcrumbs or back links for detail pages

### Global
1. **Sidebar**: Active state matches current route
2. **Auth guard**: Redirects to /login when not authenticated
3. **Token refresh**: 401 responses trigger automatic refresh
4. **Consistent badges**: Same color coding for severity/status across all pages
5. **Table pagination**: All list pages use Page<T> with page controls

## Process

1. List all pages under `src/app/`
2. For each page, check the 6 per-page items
3. Check the 5 global items
4. Report findings as a table: page | issue | severity
