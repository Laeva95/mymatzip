#!/bin/bash
# ============================================================
# [mysql-setup.sh] EC2(Ubuntu 24.04)에 MySQL 8 설치 + matzip DB·계정 생성 (부팀장, 1회 실행)
# 사용법: bash mysql-setup.sh '여기에_DB_비밀번호'
# 실행 후 /home/ubuntu/matzip/matzip.env 의 DB_PASSWORD 에 같은 비밀번호를 넣는다.
# ============================================================
set -e
DB_PASSWORD="$1"
if [ -z "$DB_PASSWORD" ]; then
  echo "사용법: bash mysql-setup.sh 'DB비밀번호'"; exit 1
fi

# 1. MySQL 8 설치 (Ubuntu 24.04 기본 저장소는 MySQL 8.0)
sudo apt update
sudo apt install -y mysql-server
sudo systemctl enable --now mysql

# 2. 외부 접속 차단 확인: 앱과 DB 가 같은 서버이므로 127.0.0.1 에서만 받는다 (기본값 유지)
#    보안 그룹에 3306 포트를 열지 않는다.
grep -E "^bind-address" /etc/mysql/mysql.conf.d/mysqld.cnf || true

# 3. DB 와 전용 계정 만들기 (utf8mb4: 한글·이모지 저장)
sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS matzip DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'matzip'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON matzip.* TO 'matzip'@'localhost';
FLUSH PRIVILEGES;
SQL

# 4. 확인
mysql -u matzip -p"${DB_PASSWORD}" -e "SELECT VERSION(); SHOW DATABASES LIKE 'matzip';"
echo "MySQL 준비 완료. matzip.env 에 DB_PASSWORD 를 넣고 sudo systemctl restart matzip"