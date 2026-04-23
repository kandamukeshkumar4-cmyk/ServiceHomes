variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "email_domain" {
  description = "Domain for SES email sending"
  type        = string
  default     = ""
}
