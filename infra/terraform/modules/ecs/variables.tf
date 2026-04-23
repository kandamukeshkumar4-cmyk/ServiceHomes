variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "api_image_uri" {
  description = "ECR image URI for the API container"
  type        = string
}

variable "task_cpu" {
  description = "ECS task CPU units"
  type        = number
}

variable "task_memory" {
  description = "ECS task memory in MB"
  type        = number
}

variable "desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
}

variable "vpc_id" {
  description = "VPC ID where ECS tasks will run"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for ECS tasks"
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "ALB security group ID allowed to reach ECS tasks"
  type        = string
}

variable "alb_target_group_arn" {
  description = "ALB target group ARN"
  type        = string
}

variable "ecs_task_execution_role_arn" {
  description = "ECS task execution role ARN"
  type        = string
}

variable "ecs_task_role_arn" {
  description = "ECS task runtime role ARN"
  type        = string
}

variable "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  type        = string
}

variable "rds_secret_arn" {
  description = "ARN of the RDS credentials secret"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "s3_bucket_name" {
  description = "S3 bucket name for media storage"
  type        = string
}

variable "auth0_domain" {
  description = "Auth0 domain"
  type        = string
  default     = ""
}

variable "auth0_audience" {
  description = "Auth0 audience"
  type        = string
  default     = ""
}
