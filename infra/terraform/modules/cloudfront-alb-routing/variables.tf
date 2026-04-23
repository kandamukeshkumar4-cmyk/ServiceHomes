variable "name" {
  description = "Stable name prefix for CloudFront resources."
  type        = string
}

variable "enabled" {
  description = "Whether the CloudFront distribution is enabled."
  type        = bool
  default     = true
}

variable "comment" {
  description = "Optional CloudFront distribution comment."
  type        = string
  default     = null
}

variable "aliases" {
  description = "Optional alternate domain names for the distribution."
  type        = list(string)
  default     = []
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN in us-east-1 for aliases. Leave null to use the CloudFront default certificate."
  type        = string
  default     = null
}

variable "web_origin_domain_name" {
  description = "Domain name for the default web origin."
  type        = string
}

variable "web_origin_id" {
  description = "Origin ID for the default web origin."
  type        = string
  default     = "web-origin"
}

variable "web_origin_protocol_policy" {
  description = "Protocol policy for the default web origin."
  type        = string
  default     = "https-only"

  validation {
    condition     = contains(["http-only", "https-only", "match-viewer"], var.web_origin_protocol_policy)
    error_message = "web_origin_protocol_policy must be http-only, https-only, or match-viewer."
  }
}

variable "web_origin_http_port" {
  description = "HTTP port for the default web origin."
  type        = number
  default     = 80
}

variable "web_origin_https_port" {
  description = "HTTPS port for the default web origin."
  type        = number
  default     = 443
}

variable "alb_domain_name" {
  description = "DNS name of the ALB that serves API and websocket traffic."
  type        = string
}

variable "alb_origin_id" {
  description = "Origin ID for the ALB."
  type        = string
  default     = "api-alb"
}

variable "alb_origin_protocol_policy" {
  description = "Protocol policy for CloudFront to ALB origin traffic."
  type        = string
  default     = "https-only"

  validation {
    condition     = contains(["http-only", "https-only", "match-viewer"], var.alb_origin_protocol_policy)
    error_message = "alb_origin_protocol_policy must be http-only, https-only, or match-viewer."
  }
}

variable "alb_http_port" {
  description = "HTTP port exposed by the ALB."
  type        = number
  default     = 80
}

variable "alb_https_port" {
  description = "HTTPS port exposed by the ALB."
  type        = number
  default     = 443
}

variable "websocket_origin_read_timeout" {
  description = "CloudFront origin read timeout for websocket handshakes and long-lived upgrade traffic."
  type        = number
  default     = 60
}

variable "default_root_object" {
  description = "Default object served by the web origin."
  type        = string
  default     = "index.html"
}

variable "price_class" {
  description = "CloudFront price class."
  type        = string
  default     = "PriceClass_100"
}

variable "web_acl_id" {
  description = "Optional AWS WAF web ACL ID."
  type        = string
  default     = null
}

variable "geo_restriction_type" {
  description = "CloudFront geo restriction type."
  type        = string
  default     = "none"

  validation {
    condition     = contains(["none", "whitelist", "blacklist"], var.geo_restriction_type)
    error_message = "geo_restriction_type must be none, whitelist, or blacklist."
  }
}

variable "geo_restriction_locations" {
  description = "ISO country codes for geo restrictions."
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags applied to CloudFront resources."
  type        = map(string)
  default     = {}
}
