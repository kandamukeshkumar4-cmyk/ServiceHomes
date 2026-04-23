output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = module.alb.alb_dns_name
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = module.rds.endpoint
}

output "ecr_api_repository_url" {
  description = "ECR repository URL for the API"
  value       = module.ecr.api_repository_url
}

output "ecr_web_repository_url" {
  description = "ECR repository URL for the web frontend"
  value       = module.ecr.web_repository_url
}

output "cloudfront_domain" {
  description = "CloudFront distribution domain name"
  value       = module.frontend.cloudfront_domain
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID"
  value       = module.frontend.cloudfront_distribution_id
}

output "s3_bucket_name" {
  description = "S3 bucket name for frontend static assets"
  value       = module.frontend.s3_bucket_name
}

output "media_bucket_name" {
  description = "S3 bucket name for media uploads"
  value       = module.frontend.media_bucket_name
}

output "ses_configuration_set_name" {
  description = "SES configuration set name"
  value       = module.ses.configuration_set_name
}
