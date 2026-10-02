-- Autenticación: el usuario demo (id 1) tenía password_hash = 'demo' en texto plano.
-- Le asigna el hash BCrypt de la contraseña 'valentina123' para que pueda iniciar sesión
-- y conservar sus eventos. No cambia el esquema.
UPDATE app_user
SET email = lower(trim(email)),
    password_hash = '$2a$10$8AYeauaUVJccuycdTvtN7OH5.cf6V6wrmE8kCz/HzVK1GRH230hKm'
WHERE id = 1;
