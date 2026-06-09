-- fix_supabase_admin.sql
-- Resets Supabase internal service-role passwords when they fall out of sync
-- with POSTGRES_PASSWORD in .env (e.g. after regenerating secrets).
--
-- Never hardcode the password here. Pass it at runtime via -v:
--
--   Windows (PowerShell):
--     $pw = (Select-String 'POSTGRES_PASSWORD' .env).Line.Split('=')[1].Trim()
--     docker exec supabase-db psql -U supabase_admin -h 127.0.0.1 -d postgres `
--       -v postgres_password="$pw" `
--       -f database/fix_supabase_admin.sql
--
--   Linux / macOS:
--     source .env
--     docker exec supabase-db psql -U supabase_admin -h 127.0.0.1 -d postgres \
--       -v postgres_password="$POSTGRES_PASSWORD" \
--       -f database/fix_supabase_admin.sql
--
-- The value of POSTGRES_PASSWORD comes from vinder-AppDev/.env

ALTER ROLE supabase_auth_admin    WITH PASSWORD :'postgres_password';
ALTER ROLE supabase_storage_admin WITH PASSWORD :'postgres_password';
ALTER ROLE authenticator          WITH PASSWORD :'postgres_password';
ALTER ROLE supabase_admin         WITH PASSWORD :'postgres_password';

GRANT ALL PRIVILEGES ON DATABASE postgres TO supabase_admin;
GRANT ALL ON SCHEMA public TO supabase_admin;
GRANT ALL ON ALL TABLES IN SCHEMA public TO supabase_admin;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO supabase_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO supabase_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO supabase_admin;
