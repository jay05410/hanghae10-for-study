# Terraform 설정
terraform {
  required_version = ">= 1.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# AWS Provider
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "ecommerce"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# 최신 Amazon Linux 2023 AMI (ARM)
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-arm64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# 가용 영역 조회
data "aws_availability_zones" "available" {
  state = "available"
}
