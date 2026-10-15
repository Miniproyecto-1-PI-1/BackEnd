-- Registrar avance: nota opcional de la gestión («esperando confirmación del salón») y fecha de ejecución.
-- Ambas son nullable: las gestiones existentes quedan sin nota y sin fecha de ejecución.
ALTER TABLE task ADD COLUMN IF NOT EXISTS note varchar(500);
ALTER TABLE task ADD COLUMN IF NOT EXISTS completed_at timestamp;
