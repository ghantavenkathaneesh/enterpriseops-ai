INSERT INTO roles(name) VALUES ('ADMIN'),('MANAGER'),('SUPPORT_AGENT'),('EMPLOYEE');
INSERT INTO sla_policies(priority, response_minutes, resolution_minutes) VALUES
 ('CRITICAL', 15, 240), ('HIGH', 60, 480), ('MEDIUM', 240, 1440), ('LOW', 480, 4320);
