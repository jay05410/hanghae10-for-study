# ===========================================
# 기본 설정
# ===========================================
variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2" # 서울
}

variable "environment" {
  description = "환경 (dev/prod)"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "프로젝트 이름"
  type        = string
  default     = "ecommerce"
}

# ===========================================
# 네트워크 설정
# ===========================================
variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

# ===========================================
# EC2 설정
# ===========================================
variable "app_instance_type" {
  description = "App 서버 인스턴스 타입"
  type        = string
  default     = "t4g.micro"
}

variable "redis_instance_type" {
  description = "Redis 서버 인스턴스 타입"
  type        = string
  default     = "t4g.micro"
}

variable "kafka_instance_type" {
  description = "Kafka 서버 인스턴스 타입"
  type        = string
  default     = "t4g.small"
}

variable "key_name" {
  description = "EC2 SSH 키 페어 이름"
  type        = string
}

# ===========================================
# RDS 설정
# ===========================================
variable "db_instance_class" {
  description = "RDS 인스턴스 클래스"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_name" {
  description = "데이터베이스 이름"
  type        = string
  default     = "ecommerce"
}

variable "db_username" {
  description = "데이터베이스 사용자명"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "데이터베이스 비밀번호"
  type        = string
  sensitive   = true
}

# ===========================================
# Redis 설정
# ===========================================
variable "redis_password" {
  description = "Redis 비밀번호"
  type        = string
  sensitive   = true
  default     = ""
}

# ===========================================
# 허용 IP 목록 (SSH 접속용)
# ===========================================
variable "allowed_ips" {
  description = "SSH 접속 허용할 IP 목록"
  type        = list(string)
  default     = []
}
