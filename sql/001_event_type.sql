-- Sprint 1: tipo de evento (Boda, Social, Corporativo, Cumpleaños, Otro).
-- Nullable para no romper los eventos que ya existen.
ALTER TABLE event ADD COLUMN IF NOT EXISTS type varchar(50);
