CREATE DATABASE matzip DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'matzip'@'localhost' IDENTIFIED BY 'matzip1234';
GRANT ALL PRIVILEGES ON matzip.* TO 'matzip'@'localhost';