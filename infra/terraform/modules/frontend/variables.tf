variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "environment" {
  description = "Deployment environment"
  type        = string
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS (optional)"
  type        = string
  default     = ""
}

variable "hosted_zone_id" {
  description = "Route53 hosted zone ID for DNS records (optional)"
  type        = string
  default     = ""
}

variable "domain_name" {
  description = "Domain name for Route53 records"
  type        = string
  default     = ""
}
