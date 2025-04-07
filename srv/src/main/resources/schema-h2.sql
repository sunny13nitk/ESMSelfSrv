
DROP VIEW IF EXISTS LogsReadService_Logs;
DROP TABLE IF EXISTS db_esmlogs_esmapplogs;

CREATE TABLE db_esmlogs_esmapplogs (
  ID NVARCHAR(36) NOT NULL,
  username NVARCHAR(50),
  casetype NVARCHAR(50),
  timestamp TIMESTAMP(7),
  status NVARCHAR(50),
  msgtype NVARCHAR(50),
  objectid NVARCHAR(50),
  message NVARCHAR(1000),
  PRIMARY KEY(ID)
); 

CREATE VIEW LogsReadService_Logs AS SELECT
  esmapplogs_0.ID,
  esmapplogs_0.username,
  esmapplogs_0.casetype,
  esmapplogs_0.timestamp,
  esmapplogs_0.status,
  esmapplogs_0.msgtype,
  esmapplogs_0.objectid,
  esmapplogs_0.message
FROM db_esmlogs_esmapplogs AS esmapplogs_0; 

