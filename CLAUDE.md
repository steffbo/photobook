# Photobook Application

A full-stack photo book application with multi-user album management, drag-and-drop uploads (including ZIP archives), and automatic thumbnail generation.

## Tech Stack

- **Backend**: Spring Boot 4, Java 25, Maven
- **Frontend**: Vue.js 3, TypeScript, Vite, shadcn-vue, Radix Vue, Tailwind CSS
- **UI Components**: shadcn-vue (Vega style, neutral base, cyan theme)
- **Icons**: Lucide Icons
- **Database**: PostgreSQL 17 with Flyway migrations
- **Object Storage**: SeaweedFS (S3-compatible)
- **Authentication**: JWT (access + refresh tokens)
- **API**: OpenAPI 3.0 spec-first approach with code generation

## Project Structure

```
photobook/
├── openapi.yaml                    # API specification (source of truth)
├── docker-compose.yml              # PostgreSQL + SeaweedFS for local dev
├── .env.example                    # Environment variables template
├── backend/
│   ├── pom.xml                     # Maven config with OpenAPI generator
│   ├── src/main/java/cc/remer/photobook/
│   │   ├── PhotobookApplication.java
│   │   ├── adapter/
│   │   │   ├── security/           # JWT auth (copied from timetrack)
│   │   │   ├── web/                # Controllers implementing generated API
│   │   │   ├── persistence/        # JPA repositories
│   │   │   └── storage/            # SeaweedFS S3 client
│   │   ├── config/
│   │   ├── domain/                 # Entities
│   │   ├── usecase/                # Business logic
│   │   └── exception/
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/           # Flyway SQL migrations
└── frontend/
    ├── package.json                # With openapi-typescript-codegen
    ├── vite.config.ts
    ├── tailwind.config.js
    ├── components.json             # shadcn-vue config
    └── src/
        ├── api/generated/          # Generated API client
        ├── components/
        │   └── ui/                 # shadcn-vue components
        ├── views/
        ├── composables/
        ├── lib/
        │   └── utils.ts            # Tailwind utilities (cn helper)
        └── stores/
```

## Database Schema

### Tables

1. **users** - User accounts with roles (ADMIN, USER)
2. **photos** - Photo metadata (SeaweedFS keys, original filename, EXIF data, sizes)
3. **photo_thumbnails** - Generated thumbnails at different resolutions
4. **albums** - Photo albums with name, description, cover photo
5. **album_photos** - Many-to-many: photos can belong to multiple albums
6. **album_users** - Access control: which users can view/edit which albums
7. **refresh_tokens** - JWT refresh token storage

### Thumbnail Resolutions

- **small**: 150px (for grid thumbnails)
- **medium**: 400px (for album view)
- **large**: 800px (for lightbox preview)
- **original**: stored as-is in SeaweedFS

## TODO List

**IMPORTANT**: After completing each TODO item, mark it as `[x]` and commit the update with a semantic commit message (see Git Workflow section).

### Phase 1: Project Setup & Infrastructure

- [ ] **1.1** Create `docker-compose.yml` with PostgreSQL 17 and SeaweedFS containers
- [ ] **1.2** Create `.env.example` with all required environment variables
- [ ] **1.3** Create `openapi.yaml` with complete API specification:
  - Authentication endpoints (login, refresh, logout)
  - User management (CRUD, admin only for create/delete)
  - Album management (CRUD, access control)
  - Photo management (upload, list, delete, move between albums)
  - Photo serving (thumbnails, originals with presigned URLs)
- [ ] **1.4** Create backend Maven project structure with `pom.xml`:
  - Spring Boot 4 parent
  - OpenAPI generator plugin (spring generator, interfaceOnly)
  - All dependencies (security, JPA, Flyway, S3, JWT, validation)
- [ ] **1.5** Initialize frontend project with shadcn-vue:
  - Run: `bunx --bun shadcn@latest create --preset "https://ui.shadcn.com/init?base=radix&style=vega&baseColor=neutral&theme=cyan&iconLibrary=lucide&font=inter&menuAccent=subtle&menuColor=default&radius=small&template=vite" --template vite`
  - This creates Vue 3 + TypeScript + Vite + Tailwind CSS + shadcn-vue setup
  - Add openapi-typescript-codegen for API client generation
  - Add axios for HTTP requests
  - Add @vueuse/core for composables (drag-and-drop, gestures, etc.)

### Phase 2: Backend Core

- [ ] **2.1** Create Flyway migration `V1__initial_schema.sql`:
  - All tables with proper constraints and indexes
  - Admin user seeded (email: admin@photobook.local, password: admin)
- [ ] **2.2** Copy and adapt authentication from timetrack:
  - `SecurityConfig.java`
  - `JwtAuthenticationFilter.java`
  - `JwtTokenProvider.java`
  - `CustomUserDetailsService.java`
  - `UserPrincipal.java`
  - `JwtProperties.java`
- [ ] **2.3** Create domain entities:
  - `User.java`
  - `Photo.java`
  - `PhotoThumbnail.java`
  - `Album.java`
  - `AlbumUser.java` (with role: OWNER, VIEWER)
  - `RefreshToken.java`
- [ ] **2.4** Create repositories for all entities
- [ ] **2.5** Create `application.yml` with all configurations

### Phase 3: Storage & Processing

- [ ] **3.1** Create SeaweedFS S3 adapter:
  - Configure S3 client for SeaweedFS
  - Upload, download, delete operations
  - Presigned URL generation
- [ ] **3.2** Create thumbnail generation service:
  - Use Thumbnailator or imgscalr library
  - Generate small (150px), medium (400px), large (800px)
  - Extract and store EXIF metadata
- [ ] **3.3** Create photo upload service:
  - Accept multipart file uploads
  - Extract ZIP archives and process contained images
  - Support folder uploads (process recursively)
  - Queue thumbnail generation (async)

### Phase 4: API Implementation

- [ ] **4.1** Implement `AuthenticationController`:
  - Login, refresh token, logout endpoints
- [ ] **4.2** Implement `UserController`:
  - Get current user, update profile
  - Admin: list users, create user, delete user
- [ ] **4.3** Implement `AlbumController`:
  - CRUD operations with access control
  - List albums for current user
  - Manage album access (add/remove users)
- [ ] **4.4** Implement `PhotoController`:
  - Upload photos (single, multiple, ZIP)
  - List photos in album
  - Delete photos
  - Move/copy photos between albums
  - Get photo URLs (thumbnails, original)

### Phase 5: Frontend Core & Design System

- [ ] **5.1** Set up Vue project with router, stores, and API client generation:
  - Install and configure Vue Router
  - Install and configure Pinia for state management
  - Set up openapi-typescript-codegen script in package.json
  - Configure axios interceptors for JWT handling
- [ ] **5.2** Configure shadcn-vue design system:
  - Verify Tailwind config uses consistent spacing (already uses 4px base)
  - Extend Tailwind theme in `tailwind.config.js` to match 60-30-10 color rule:
    - Background colors (neutral - 60%): surfaces, cards, main areas
    - Muted colors (neutral variant - 30%): sidebars, secondary surfaces
    - Primary/Accent colors (cyan - 10%): CTAs, links, focus states
  - Add custom Tailwind utilities for common spacing patterns
  - Document responsive breakpoints (sm: 640px, md: 768px, lg: 1024px, xl: 1280px)
  - Install base shadcn-vue components: Button, Card, Dialog, Input, Label, Skeleton, Toast
- [ ] **5.3** Create authentication flow:
  - Login page with responsive layout using shadcn-vue Card and Input
  - JWT storage and refresh logic with composable
  - Auth guard for protected routes
  - Implement toast notifications for auth feedback
- [ ] **5.4** Create layout components:
  - App shell with responsive navigation using shadcn-vue Sheet (mobile) and Sidebar (desktop)
  - Collapsible sidebar with smooth transitions
  - Use Tailwind spacing utilities consistently (p-4, gap-4, etc.)

### Phase 6: Frontend Features

- [ ] **6.1** Create album management views:
  - Install shadcn-vue components: Badge, DropdownMenu, Avatar
  - Album list with Tailwind grid (grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4)
  - Album cards using shadcn-vue Card with hover effects (hover:shadow-lg transition)
  - Album detail view with responsive photo grid
  - Create/edit album using shadcn-vue Dialog with Sheet on mobile
  - Manage album access dialog with user selection
  - Empty state component with Lucide icons and "Create Album" Button
- [ ] **6.2** Create photo upload component:
  - Install @vueuse/core for useDropZone composable
  - Install additional shadcn-vue components: Progress
  - Drag-and-drop zone with visual feedback (border-dashed, border-2, hover:border-primary)
  - Upload progress using shadcn-vue Progress component
  - Error handling with toast notifications and retry button
  - Responsive: Dialog (desktop) or Sheet (mobile)
- [ ] **6.3** Create photo gallery component:
  - Responsive grid using Tailwind (grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6)
  - Skeleton loaders using shadcn-vue Skeleton
  - Custom lightbox viewer component (or use vue-easy-lightbox library)
  - Multi-select with shadcn-vue Checkbox overlays
  - Floating action toolbar with shadcn-vue DropdownMenu for batch operations
  - Empty state with Lucide ImageOff icon
- [ ] **6.4** Create admin views:
  - Install shadcn-vue Table component
  - User management with responsive Table (cards on mobile using shadcn-vue Card)
  - Dashboard with stats cards using shadcn-vue Card and Badge

### Phase 7: UX Polish & Responsiveness

- [ ] **7.1** Add loading and empty states:
  - Skeleton screens using shadcn-vue Skeleton with proper aspect ratios
  - Empty state components with Lucide icons and descriptive text
  - Loading Button states (Button with disabled and loading props)
- [ ] **7.2** Add keyboard navigation:
  - Use @vueuse/core useMagicKeys for keyboard shortcuts
  - Arrow keys for photo navigation in lightbox
  - Delete key for removing selected photos (with AlertDialog confirmation)
  - Escape key to close dialogs/lightbox
  - Enter to confirm actions
  - Focus management with Radix Vue's built-in focus trap
- [ ] **7.3** Implement responsive behavior:
  - Test all views on mobile (320px), tablet (768px - md), desktop (1024px+ - lg)
  - Touch-friendly hit targets using Tailwind (min-h-11 for mobile buttons)
  - Swipe gestures with @vueuse/core useSwipe for mobile photo navigation
  - Adaptive navigation (Sheet on mobile, Sidebar on desktop)
  - Responsive typography using Tailwind (text-sm md:text-base)
- [ ] **7.4** Add micro-interactions:
  - Page transitions using Vue's Transition component
  - Hover effects with Tailwind (hover:scale-105, hover:brightness-110)
  - Loading states with opacity and pulse animations
  - Toast notifications with shadcn-vue Sonner (smooth slide-in animations)
- [ ] **7.5** Add optimistic UI updates:
  - Immediate UI updates with Pinia store mutations
  - Rollback on error with toast notification
  - Loading skeletons during operations
- [ ] **7.6** Add undo functionality:
  - Custom toast with action button for undo
  - 5-second timer before permanent deletion
  - Store deleted items in temporary state
- [ ] **7.7** Accessibility audit:
  - ARIA labels on icon-only buttons (aria-label attribute)
  - Keyboard navigation testing (all Radix Vue components have built-in support)
  - Screen reader testing with VoiceOver/NVDA
  - Color contrast validation (shadcn-vue themes are WCAG AA compliant)
  - Focus-visible styles using Tailwind (focus-visible:ring-2)

## Design Guidelines

### Color System (60-30-10 Rule with Tailwind & shadcn-vue)

The shadcn-vue preset (Vega style, neutral base, cyan theme) provides a color system that naturally follows the 60-30-10 rule:

- **Background/Neutral (60%)**: Used for main surfaces, large areas
  - Tailwind classes: `bg-background`, `bg-card`, `bg-muted`
  - Example: page backgrounds, card surfaces, main content areas
  - Neutral base ensures photos are the visual focus

- **Muted/Secondary (30%)**: Used for secondary surfaces and supporting content
  - Tailwind classes: `bg-muted`, `text-muted-foreground`, `bg-secondary`
  - Example: sidebars, secondary buttons, disabled states, borders

- **Primary/Accent (10%)**: Cyan theme for interactive elements and CTAs
  - Tailwind classes: `bg-primary`, `text-primary`, `border-primary`, `hover:bg-primary/90`
  - Example: primary buttons, links, selected states, focus rings
  - Use sparingly to draw attention to key actions

**Color Usage Rules:**
- Use shadcn-vue's semantic color tokens (background, foreground, primary, secondary, muted, accent, destructive)
- Never hardcode hex colors - always use Tailwind's theme colors
- Colors are defined in `tailwind.config.js` and automatically work in light/dark mode
- Use opacity modifiers for subtle variations: `bg-primary/10`, `text-foreground/70`

Example color configuration in `tailwind.config.js`:
```js
module.exports = {
  theme: {
    extend: {
      colors: {
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))", // Cyan - 10%
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))", // Neutral variant - 30%
          foreground: "hsl(var(--secondary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))", // Part of 60%
          foreground: "hsl(var(--muted-foreground))",
        },
        // ... other semantic colors
      }
    }
  }
}
```

### Spacing System (Tailwind's 4px Base Scale)

Tailwind CSS uses a 4px base spacing scale by default. Use these spacing utilities consistently:

- **1 (4px)**: `p-1`, `gap-1`, `space-x-1` - Tight spacing (icon-to-text)
- **2 (8px)**: `p-2`, `gap-2`, `m-2` - Small spacing (button internal padding)
- **3 (12px)**: `p-3`, `gap-3` - Compact spacing (card padding on mobile)
- **4 (16px)**: `p-4`, `gap-4`, `m-4` - Medium spacing (default card padding, form groups)
- **6 (24px)**: `p-6`, `gap-6`, `m-6` - Large spacing (section gaps, card margins)
- **8 (32px)**: `p-8`, `gap-8`, `m-8` - Extra large (page padding on desktop)
- **12 (48px)**: `p-12`, `gap-12` - Section dividers
- **16 (64px)**: `p-16`, `gap-16` - Major layout gaps

**Spacing Rules:**
- Never use arbitrary values like `p-[13px]` - always use the scale above
- Prefer gap utilities for flexbox/grid layouts: `flex gap-4`
- Use responsive spacing: `p-4 md:p-6 lg:p-8`
- Common patterns:
  - Cards: `p-4 md:p-6` (16px mobile, 24px desktop)
  - Page container: `p-4 md:p-6 lg:p-8`
  - Button groups: `flex gap-2` (8px between buttons)
  - Form fields: `space-y-4` (16px vertical spacing)

### Responsive Breakpoints (Tailwind Default)

Tailwind CSS provides a mobile-first breakpoint system:

- **Mobile (default)**: < 640px - Base styles, single column layouts, stacked navigation
- **sm**: ≥ 640px - Small tablets, 2 column grids
- **md**: ≥ 768px - Tablets, 2-3 column layouts, collapsible sidebar
- **lg**: ≥ 1024px - Desktop, 3-4 column layouts, persistent sidebar
- **xl**: ≥ 1280px - Large desktop, 4-6 column layouts
- **2xl**: ≥ 1536px - Extra large screens

**Responsive Patterns:**
```html
<!-- Mobile-first: 1 col mobile, 2 cols tablet, 3 cols desktop -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">

<!-- Responsive padding: 16px mobile, 24px tablet, 32px desktop -->
<div class="p-4 md:p-6 lg:p-8">

<!-- Responsive text: small mobile, base desktop -->
<p class="text-sm md:text-base">

<!-- Hide on mobile, show on desktop -->
<div class="hidden lg:block">
```

### Typography (Inter Font + Tailwind)

The shadcn-vue preset uses Inter font with Tailwind's typography scale:

- **Font sizes**: `text-xs` (12px), `text-sm` (14px), `text-base` (16px), `text-lg` (18px), `text-xl` (20px), `text-2xl` (24px)
- **Line height**: Tailwind auto-adjusts (1.5 for body, 1.2 for headings)
- **Font weights**: `font-normal` (400), `font-medium` (500), `font-semibold` (600), `font-bold` (700)
- **Responsive typography**: Use responsive prefixes: `text-sm md:text-base lg:text-lg`

Example:
```html
<h1 class="text-2xl md:text-3xl lg:text-4xl font-bold">Page Title</h1>
<p class="text-sm md:text-base text-muted-foreground">Description</p>
```

### Component Guidelines (shadcn-vue + Tailwind)

- **Touch targets**: Use `min-h-11` (44px) on mobile buttons: `<Button class="min-h-11 md:min-h-10">`
- **Cards**: Use shadcn-vue Card with responsive padding: `<Card class="p-4 md:p-6">`
- **Buttons**: Use shadcn-vue Button with variants: `variant="default"` (primary), `variant="secondary"`, `variant="outline"`, `variant="ghost"`
- **Forms**: Use shadcn-vue form components (Input, Label, Textarea) with consistent spacing
- **Icons**: Use Lucide icons exclusively: `import { Upload, ImageOff, Trash2 } from 'lucide-vue-next'`
- **Dialogs**: Use Dialog for desktop, Sheet for mobile: `<Dialog class="hidden md:block">` / `<Sheet class="md:hidden">`

### Accessibility Requirements

shadcn-vue components built on Radix Vue provide excellent accessibility out of the box:

- **Color contrast**: shadcn-vue themes are WCAG AA compliant by default
- **Focus indicators**: Use Tailwind's focus-visible utilities: `focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2`
- **ARIA labels**: Add to icon-only buttons: `<Button aria-label="Upload photos"><Upload /></Button>`
- **Keyboard navigation**: Radix Vue components handle keyboard navigation automatically (Tab, Enter, Escape, Arrow keys)
- **Focus management**: Dialogs and Sheets trap focus automatically
- **Screen reader support**: Radix Vue provides proper ARIA attributes and roles

## Running Locally

### Start Infrastructure

```bash
docker compose up -d
```

This starts:
- PostgreSQL on port 5432
- SeaweedFS Master on port 9333
- SeaweedFS Volume on port 8080
- SeaweedFS Filer (S3) on port 8333

### Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend runs on http://localhost:8081

### Start Frontend

```bash
cd frontend
bun install  # or npm install
bun run dev  # or npm run dev
```

Frontend runs on http://localhost:5173

### Generate API Clients

After modifying `openapi.yaml`:

```bash
# Backend (runs automatically with mvn compile)
cd backend && ./mvnw compile

# Frontend
cd frontend && bun run generate-api  # or npm run generate-api
```

### Add shadcn-vue Components

When you need a new component:

```bash
cd frontend
bunx shadcn-vue@latest add button
bunx shadcn-vue@latest add card
bunx shadcn-vue@latest add dialog
# etc.
```

## Git Workflow

### Remote Repository

The project is hosted at: `git@github.com:steffbo/photobook.git`

### Commit Convention (Semantic Commits)

Use semantic commit messages following the Conventional Commits specification:

**Format**: `<type>(<scope>): <description>`

**Types:**
- `feat`: New feature for the user
- `fix`: Bug fix for the user
- `docs`: Documentation changes
- `style`: Code style changes (formatting, missing semicolons, etc.)
- `refactor`: Code refactoring (neither fixes a bug nor adds a feature)
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `build`: Changes to build system or dependencies
- `ci`: CI/CD configuration changes
- `chore`: Other changes that don't modify src or test files

**Scope** (optional): The area of the codebase affected (e.g., `auth`, `albums`, `photos`, `ui`, `api`)

**Examples:**
```bash
git commit -m "feat(auth): implement JWT authentication with refresh tokens"
git commit -m "feat(albums): add album creation and management views"
git commit -m "fix(photos): resolve upload progress not updating"
git commit -m "docs: update README with shadcn-vue setup instructions"
git commit -m "style(ui): apply consistent spacing to card components"
git commit -m "refactor(api): extract API client into composable"
git commit -m "build: add @vueuse/core dependency for composables"
```

### Workflow After Completing a TODO

After completing each TODO item:

1. **Stage your changes:**
   ```bash
   git add .
   ```

2. **Commit with semantic message:**
   ```bash
   git commit -m "feat(scope): description of what was implemented"
   ```
   - Reference the TODO phase/number in the description if helpful
   - Example: `feat(frontend): initialize shadcn-vue project (phase 1.5)`

3. **Update CLAUDE.md TODO list:**
   - Mark the completed TODO as `[x]` instead of `[ ]`
   - Commit the documentation update:
   ```bash
   git add CLAUDE.md
   git commit -m "docs: mark TODO X.X as completed"
   ```

4. **Push to remote:**
   ```bash
   git push origin main
   ```

### Example Complete Workflow

```bash
# Complete a feature
git add .
git commit -m "feat(auth): implement login page with JWT storage"

# Update TODO list
# (Edit CLAUDE.md to mark task 5.3 as complete)
git add CLAUDE.md
git commit -m "docs: mark TODO 5.3 (auth flow) as completed"

# Push all commits
git push origin main
```

### Branch Strategy

For this project, work directly on `main` branch since it's a single-developer project. For larger features, you may create feature branches:

```bash
git checkout -b feat/album-management
# ... make changes ...
git commit -m "feat(albums): implement album CRUD operations"
git push origin feat/album-management
# Create PR on GitHub, then merge to main
```

## Environment Variables

See `.env.example` for all configuration options:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - PostgreSQL connection
- `SEAWEEDFS_S3_ENDPOINT` - SeaweedFS S3 API endpoint
- `SEAWEEDFS_ACCESS_KEY`, `SEAWEEDFS_SECRET_KEY` - S3 credentials
- `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION` - JWT config
- `SERVER_PORT` - Backend port (default: 8081)

## API Design Notes

### Album Access Control

- Albums have an owner (creator) who has full control
- Owner can grant access to other users (VIEWER role)
- VIEWER can view photos but not upload/delete
- Admin can see and manage all albums

### Photo Storage

- Original photos stored in SeaweedFS bucket: `photobook-originals`
- Thumbnails stored in SeaweedFS bucket: `photobook-thumbnails`
- Photo paths: `{userId}/{albumId}/{photoId}.{ext}`
- Thumbnail paths: `{userId}/{albumId}/{photoId}_{size}.jpg`

### Upload Flow

1. Frontend sends multipart POST to `/api/photos/upload`
2. Backend extracts ZIP if needed, validates image files
3. Original stored in SeaweedFS immediately
4. Photo record created in DB with status `PROCESSING`
5. Async job generates thumbnails
6. Status updated to `READY` when complete
