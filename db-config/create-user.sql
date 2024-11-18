CREATE USER 'documentDb'@'localhost' IDENTIFIED BY "cps510DocumentManagementSystem";
GRANT ALL PRIVILEGES ON documents.* to 'documentDb'@'localhost';