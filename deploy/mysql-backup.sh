#!/bin/bash
# [mysql-backup.sh] matzip DB 를 파일로 백업 (발표 전, 큰 수정 전에 실행)
# 사용법: bash mysql-backup.sh  → /home/ubuntu/matzip/backup/matzip-날짜.sql
# 복원:   mysql -u matzip -p matzip < 백업파일.sql
set -e
source /home/ubuntu/matzip/matzip.env
mkdir -p /home/ubuntu/matzip/backup
FILE=/home/ubuntu/matzip/backup/matzip-$(date +%Y%m%d-%H%M).sql
mysqldump -u "${DB_USERNAME:-matzip}" -p"${DB_PASSWORD}" --no-tablespaces matzip > "$FILE"
echo "백업 완료: $FILE"