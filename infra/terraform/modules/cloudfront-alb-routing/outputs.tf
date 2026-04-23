output "distribution_id" {
  description = "CloudFront distribution ID."
  value       = aws_cloudfront_distribution.this.id
}

output "distribution_arn" {
  description = "CloudFront distribution ARN."
  value       = aws_cloudfront_distribution.this.arn
}

output "distribution_domain_name" {
  description = "CloudFront distribution domain name."
  value       = aws_cloudfront_distribution.this.domain_name
}

output "hosted_zone_id" {
  description = "CloudFront hosted zone ID for alias records."
  value       = aws_cloudfront_distribution.this.hosted_zone_id
}

output "websocket_origin_request_policy_id" {
  description = "Origin request policy used by the /ws/* behavior."
  value       = aws_cloudfront_origin_request_policy.websocket.id
}
