variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "rds_secret_arn" {
  description = "ARN of the RDS credentials secret"
  type        = string
  default     = ""
}

variable "s3_bucket_arn" {
  description = "ARN of the S3 bucket for media storage"
  type        = string
  default     = ""
}
