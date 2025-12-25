# ===========================================
# RDS MySQL (Free Tier)
# ===========================================
resource "aws_db_instance" "mysql" {
  identifier     = "${var.project_name}-mysql"
  engine         = "mysql"
  engine_version = "8.0.40"

  instance_class    = var.db_instance_class
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  # Free Tier 설정
  multi_az               = false
  publicly_accessible    = false
  skip_final_snapshot    = true
  deletion_protection    = false

  # 백업 설정
  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "Mon:04:00-Mon:05:00"

  # 파라미터
  parameter_group_name = aws_db_parameter_group.mysql.name

  tags = {
    Name = "${var.project_name}-mysql"
  }
}

# ===========================================
# RDS Parameter Group
# ===========================================
resource "aws_db_parameter_group" "mysql" {
  name   = "${var.project_name}-mysql-params"
  family = "mysql8.0"

  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }

  parameter {
    name  = "collation_server"
    value = "utf8mb4_unicode_ci"
  }

  parameter {
    name  = "time_zone"
    value = "Asia/Seoul"
  }

  tags = {
    Name = "${var.project_name}-mysql-params"
  }
}
