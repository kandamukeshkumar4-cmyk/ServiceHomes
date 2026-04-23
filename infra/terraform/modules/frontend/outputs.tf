output "cloudfront_domain" {
  description = "CloudFront distribution domain name"
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID"
  value       = aws_cloudfront_distribution.frontend.id
}

output "cloudfront_arn" {
  description = "CloudFront distribution ARN"
  value       = aws_cloudfront_distribution.frontend.arn
}

output "s3_bucket_name" {
  description = "S3 bucket name for frontend static assets"
  value       = aws_s3_bucket.frontend.id
}

output "s3_bucket_arn" {
  description = "S3 bucket ARN for frontend static assets"
  value       = aws_s3_bucket.frontend.arn
}

output "media_bucket_name" {
  description = "S3 bucket name for media uploads"
  value       = aws_s3_bucket.media.id
}

output "media_bucket_arn" {
  description = "S3 bucket ARN for media uploads"
  value       = aws_s3_bucket.media.arn
}
