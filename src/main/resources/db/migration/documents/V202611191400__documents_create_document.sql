create table documents.document (
  id uuid primary key,
  kind text not null check (kind in ('sbc', 'dental', 'vision', 'brochure')),
  sha256 char(64) not null unique,
  bytes bigint not null check (bytes > 0),
  page_count int not null check (page_count > 0),
  storage_name text not null unique,
  uploaded_at timestamptz not null
);
