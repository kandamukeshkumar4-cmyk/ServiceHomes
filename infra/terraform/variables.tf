variable "environment" {
  description = "Deployment environment (staging or production)"
  type        = string
  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "Environment must be either 'staging' or 'production'."
  }
}

variable "aws_region" {
  description = "AWS region for resource provisioning"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name used for resource naming and tagging"
  type        = string
  default     = "servicehomes"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "availability_zones" {
  description = "Availability zones for subnet distribution"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "ecs_task_cpu" {
  description = "ECS task CPU units"
  type        = number
  default     = 256
}

variable "ecs_task_memory" {
  description = "ECS task memory in MB"
  type        = number
  default     = 512
}

variable "ecs_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
  default     = 1
}

variable "api_image_uri" {
  description = "ECR image URI for the API container"
  type        = string
  default     = ""
}

variable "auth0_domain" {
  description = "Auth0 domain for authentication"
  type        = string
  default     = ""
}

variable "auth0_audience" {
  description = "Auth0 audience for API authorization"
  type        = string
  default     = ""
}

variable "email_domain" {
  description = "Domain for SES email sending"
  type        = string
  default     = ""
}

variable "hosted_zone_id" {
  description = "Route53 hosted zone ID for DNS records (optional)"
  type        = string
  default     = ""
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS (optional for staging)"
  type        = string
  default     = ""
}

variable "db_name" {
  description = "Name of the PostgreSQL database"
  type        = string
  default     = "servicehomes"
}

variable "db_username" {
  description = "Master username for RDS PostgreSQL"
  type        = string
  default     = "servicehomes_admin"
}
