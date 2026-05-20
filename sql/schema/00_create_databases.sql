-- PonyFlux Pay — 创建数据库（执行一次即可）
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `payflow_admin`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `payflow_cashier`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
