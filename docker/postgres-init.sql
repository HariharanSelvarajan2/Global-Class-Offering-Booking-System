SELECT 'CREATE DATABASE course_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'course_db')\gexec

SELECT 'CREATE DATABASE booking_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'booking_db')\gexec
