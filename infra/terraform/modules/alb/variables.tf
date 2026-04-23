variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where ALB will be deployed"
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs for ALB placement"
  type        = list(string)
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS (optional for staging)"
  type        = string
  default     = ""
}
