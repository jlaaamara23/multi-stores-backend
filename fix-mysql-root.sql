-- Run this in MySQL Workbench while connected to "Local instance MySQL80" (port 3307).
-- Execute each block separately (select the lines, then Execute).

-- 1) See which root users exist (run this first)
-- SELECT user, host FROM mysql.user WHERE user = 'root';

-- 2) Set password for root. Run ONLY the line that matches your root user from step 1.
--    If you have 'root'@'localhost':
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'Jlaa1357';

--    If you have 'root'@'127.0.0.1' (run in addition or instead):
-- ALTER USER 'root'@'127.0.0.1' IDENTIFIED WITH mysql_native_password BY 'Jlaa1357';

--    If you have 'root'@'%' (run in addition or instead):
-- ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'Jlaa1357';

FLUSH PRIVILEGES;

-- 3) Create database for the app
CREATE DATABASE IF NOT EXISTS multi_stores;
