# ===========================================
# App Server (On-Demand)
# ===========================================
resource "aws_instance" "app" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.app_instance_type
  key_name               = var.key_name
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.app.id]

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
    #!/bin/bash
    # Docker 설치
    yum update -y
    yum install -y docker
    systemctl start docker
    systemctl enable docker
    usermod -aG docker ec2-user

    # Docker Compose 설치
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # Git 설치
    yum install -y git
  EOF

  tags = {
    Name = "${var.project_name}-app"
    Role = "app"
  }
}

# ===========================================
# Redis Server (Spot Instance)
# ===========================================
resource "aws_spot_instance_request" "redis" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.redis_instance_type
  key_name               = var.key_name
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.redis.id]

  spot_type            = "persistent"
  wait_for_fulfillment = true

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
    #!/bin/bash
    # Docker 설치
    yum update -y
    yum install -y docker
    systemctl start docker
    systemctl enable docker
    usermod -aG docker ec2-user

    # Redis 컨테이너 실행
    docker run -d \
      --name redis \
      --restart always \
      -p 6379:6379 \
      -v redis_data:/data \
      redis:8.2-alpine \
      redis-server --appendonly yes
  EOF

  tags = {
    Name = "${var.project_name}-redis"
    Role = "redis"
  }
}

# Spot 인스턴스에 태그 추가
resource "aws_ec2_tag" "redis_name" {
  resource_id = aws_spot_instance_request.redis.spot_instance_id
  key         = "Name"
  value       = "${var.project_name}-redis"
}

# ===========================================
# Kafka Server (Spot Instance)
# ===========================================
resource "aws_spot_instance_request" "kafka" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.kafka_instance_type
  key_name               = var.key_name
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.kafka.id]

  spot_type            = "persistent"
  wait_for_fulfillment = true

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  user_data = <<-EOF
    #!/bin/bash
    # Docker 설치
    yum update -y
    yum install -y docker
    systemctl start docker
    systemctl enable docker
    usermod -aG docker ec2-user

    # Docker Compose 설치
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # Kafka 디렉토리 생성
    mkdir -p /home/ec2-user/kafka
    cd /home/ec2-user/kafka

    # docker-compose.yml 생성
    cat > docker-compose.yml <<'COMPOSE'
    version: '3.8'
    services:
      kafka:
        image: confluentinc/cp-kafka:8.1.0
        container_name: kafka
        ports:
          - "9092:9092"
        environment:
          KAFKA_NODE_ID: 1
          KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):9092
          KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:9093,PLAINTEXT_HOST://0.0.0.0:9092
          KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
          KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
          KAFKA_PROCESS_ROLES: broker,controller
          KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
          KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
          KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
          KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
          CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
        volumes:
          - kafka_data:/var/lib/kafka/data

      kafka-ui:
        image: provectuslabs/kafka-ui:latest
        container_name: kafka-ui
        ports:
          - "8090:8080"
        environment:
          KAFKA_CLUSTERS_0_NAME: ecommerce
          KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
        depends_on:
          - kafka

    volumes:
      kafka_data:
    COMPOSE

    chown -R ec2-user:ec2-user /home/ec2-user/kafka

    # Kafka 시작
    cd /home/ec2-user/kafka
    docker-compose up -d
  EOF

  tags = {
    Name = "${var.project_name}-kafka"
    Role = "kafka"
  }
}

# Spot 인스턴스에 태그 추가
resource "aws_ec2_tag" "kafka_name" {
  resource_id = aws_spot_instance_request.kafka.spot_instance_id
  key         = "Name"
  value       = "${var.project_name}-kafka"
}
