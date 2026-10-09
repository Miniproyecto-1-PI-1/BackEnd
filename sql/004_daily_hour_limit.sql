-- Planificación: límite diario de horas de gestión por organizador (6 por defecto, entre 1 y 16).
-- En una base que ya tenga la columna, la primera línea no hace nada; la restricción solo se agrega una vez.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS daily_hour_limit integer NOT NULL DEFAULT 6;
ALTER TABLE app_user ADD CONSTRAINT app_user_daily_hour_limit_range CHECK (daily_hour_limit BETWEEN 1 AND 16);
