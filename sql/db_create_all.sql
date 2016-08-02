--COMMENT
DROP SEQUENCE seq_tbl_session_id;
DROP SEQUENCE seq_tbl_request_id;

CREATE SEQUENCE seq_tbl_session_id INCREMENT 1 START 0;
CREATE SEQUENCE seq_tbl_request_id INCREMENT 1 START 0;

DROP TABLE IF EXISTS tbl_user;
DROP TABLE IF EXISTS tbl_session;
DROP TABLE IF EXISTS tbl_request;
DROP TABLE IF EXISTS tbl_condition;

--=============================================================================
CREATE TABLE tbl_user
(
	id 		INT NOT NULL UNIQUE,
	username 	STRING(256) NOT NULL UNIQUE,
	password_hash 	STRING(256) NOT NULL,
	id_condition	INT NOT NULL
);
--INSERT INTO tbl_user (id,username,password_hash,allow) VALUES (0,'tom','abcd',0);

--echo -n 'aa987r234hap8=)(/nfd9f87abcd' | sha1sum | tr a-z A-Z
--997885ADEF635172CA11908A47698D10D639795C  -

--abcd
INSERT INTO tbl_user (id,username,password_hash,id_condition) VALUES (0,'tom','997885ADEF635172CA11908A47698D10D639795C',0);
--xxx
INSERT INTO tbl_user (id,username,password_hash,id_condition) VALUES (1,'tommy','228474F77C8131174B58A5E4D4C33FB9D6066882',1);

--=============================================================================
CREATE TABLE tbl_session
(
	id 		INT NOT NULL UNIQUE,
	id_user 	STRING(256) NOT NULL,
	hash 		STRING(256) NOT NULL,
	created		BIGINT NOT NULL,
	last_access	BIGINT NOT NULL,
	logout		BIGINT NOT NULL
);
--INSERT INTO tbl_session (id,id_user,hash,created,last_access,logout) VALUES (0,0,'abcd',1,2,0);

--=============================================================================
CREATE TABLE tbl_request
(
	id 		INT NOT NULL UNIQUE,
	id_user 	STRING(256) NOT NULL,
	created		BIGINT NOT NULL,
	protocol	STRING(8) NOT NULL,
	scheme		STRING(8) NOT NULL,
	remote_addr	STRING(16) NOT NULL,
	remote_host	STRING(256) NOT NULL,
	is_secure	BOOLEAN NOT NULL,
	user_agent 	STRING(1024) NOT NULL,
	server_name	STRING(256) NOT NULL,
	server_port	INT NOT NULL,
	method		STRING(8) NOT NULL,
	content_length	INT NOT NULL,
	content_type	STRING(256),
	uri		STRING(256) NOT NULL,
	context_path	STRING(256) NOT NULL,
	path_info	STRING(256) NOT NULL,
	query_string 	STRING(1024) 
);
--INSERT INTO tbl_request (id,id_user,created,protocol,scheme,remote_addr,remote_host,is_secure
--		,user_agent,server_name,server_port,method,uri,query_string) 
--	VALUES (0,0,0,'','','','',true,'','',0,'',0,'','','','','');

--=============================================================================
CREATE TABLE tbl_condition
(
	id 		INT NOT NULL UNIQUE,
	name		STRING(256) NOT NULL UNIQUE,
	condition 	STRING(1024) NOT NULL
);
INSERT INTO tbl_condition (id,name,condition) VALUES (0,'always true','1==1');
INSERT INTO tbl_condition (id,name,condition) VALUES (1,'always false','1==0');

INSERT INTO tbl_condition (id,name,condition) VALUES (2,'rule 1','server_port=9081');
INSERT INTO tbl_condition (id,name,condition) VALUES (3,'rule 2','server_port=9081 or server_port=9082');

--=============================================================================
DROP VIEW v_user_condition;
CREATE VIEW v_user_condition AS 
	SELECT 
		a.*
		,b.condition
	FROM
		tbl_user as a
		,tbl_condition as b
	WHERE
		a.id_condition=b.id;

--=============================================================================
DROP VIEW v_session_expiry;
CREATE VIEW v_session_expiry AS 
	SELECT 
		*,( last_access+60*1000 - TONUMBER(CURRENT_TIMESTAMP) ) AS expires_in
	FROM
		tbl_session;

--=============================================================================
DROP VIEW v_session_expired;
CREATE VIEW v_session_expired AS 
	SELECT 
		*
	FROM
		v_session_expiry
	WHERE
		logout==1
	OR
		expires_in < 0;

--=============================================================================
DROP VIEW v_session_current;
CREATE VIEW v_session_current AS 
	SELECT
		*
	FROM
		v_session_expiry		
	WHERE
		logout==0
	AND
		expires_in > 0;

--EOF
