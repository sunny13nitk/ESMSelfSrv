
DROP VIEW IF EXISTS LogsReadService_Logs;
DROP TABLE IF EXISTS db_esmlogs_baseconfig;
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

CREATE TABLE db_esmlogs_baseconfig (
  caseTypeEnum NVARCHAR(50) NOT NULL,
  casetype NVARCHAR(10),
  appNotesTypes NVARCHAR(50),
  statusSchema NVARCHAR(5),
  replyNoteType NVARCHAR(50),
  catgRankEnabled BOOLEAN,
  catgsranksonlyShow BOOLEAN,
  confirmStatus NVARCHAR(20),
  fragmentHead NVARCHAR(10),
  fragmentTitle NVARCHAR(10),
  fragmentFooter NVARCHAR(10),
  caseFormView NVARCHAR(50),
  logouturl NVARCHAR(255),
  PRIMARY KEY(caseTypeEnum)
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

