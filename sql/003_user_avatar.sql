-- Configuración: foto de perfil del usuario como data URL (JPEG 256x256 comprimido en el navegador).
-- Nullable: sin foto se muestran las iniciales.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS avatar text;
