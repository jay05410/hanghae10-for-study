# ===========================================
# 출력값
# ===========================================

output "app_public_ip" {
  description = "App 서버 Public IP"
  value       = aws_instance.app.public_ip
}

output "app_public_dns" {
  description = "App 서버 Public DNS"
  value       = aws_instance.app.public_dns
}

output "redis_private_ip" {
  description = "Redis 서버 Private IP"
  value       = aws_spot_instance_request.redis.private_ip
}

output "redis_public_ip" {
  description = "Redis 서버 Public IP"
  value       = aws_spot_instance_request.redis.public_ip
}

output "kafka_public_ip" {
  description = "Kafka 서버 Public IP"
  value       = aws_spot_instance_request.kafka.public_ip
}

output "rds_endpoint" {
  description = "RDS MySQL 엔드포인트"
  value       = aws_db_instance.mysql.endpoint
}

output "rds_address" {
  description = "RDS MySQL 호스트"
  value       = aws_db_instance.mysql.address
}

# ===========================================
# 접속 정보 요약
# ===========================================
output "connection_info" {
  description = "서비스 접속 정보"
  sensitive   = true
  value = <<-EOT

    ============================================
    🖥️  App Server
    ============================================
    SSH:  ssh -i ~/.ssh/${var.key_name}.pem ec2-user@${aws_instance.app.public_ip}
    HTTP: http://${aws_instance.app.public_ip}:8080

    ============================================
    📦  Redis Server (Spot)
    ============================================
    SSH:  ssh -i ~/.ssh/${var.key_name}.pem ec2-user@${aws_spot_instance_request.redis.public_ip}
    Host: ${aws_spot_instance_request.redis.private_ip}:6379

    ============================================
    📨  Kafka Server (Spot)
    ============================================
    SSH:  ssh -i ~/.ssh/${var.key_name}.pem ec2-user@${aws_spot_instance_request.kafka.public_ip}
    Kafka: ${aws_spot_instance_request.kafka.public_ip}:9092
    UI:   http://${aws_spot_instance_request.kafka.public_ip}:8090

    ============================================
    🗄️  RDS MySQL
    ============================================
    Host: ${aws_db_instance.mysql.address}
    Port: 3306
    Database: ${var.db_name}

    ============================================
    📝  Application 환경변수
    ============================================
    DB_HOST=${aws_db_instance.mysql.address}
    DB_PORT=3306
    DB_NAME=${var.db_name}
    DB_USERNAME=${var.db_username}
    REDIS_HOST=${aws_spot_instance_request.redis.private_ip}
    REDIS_PORT=6379
    KAFKA_BOOTSTRAP_SERVERS=${aws_spot_instance_request.kafka.public_ip}:9092

  EOT
}
