output "api_repository_url" {
  description = "ECR repository URL for the API"
  value       = aws_ecr_repository.api.repository_url
}

output "web_repository_url" {
  description = "ECR repository URL for the web frontend"
  value       = aws_ecr_repository.web.repository_url
}

output "api_repository_arn" {
  description = "ECR repository ARN for the API"
  value       = aws_ecr_repository.api.arn
}

output "web_repository_arn" {
  description = "ECR repository ARN for the web frontend"
  value       = aws_ecr_repository.web.arn
}
