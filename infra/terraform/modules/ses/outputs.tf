output "verification_token" {
  description = "SES domain verification token (add as TXT record in DNS)"
  value       = var.email_domain != "" ? aws_ses_domain_identity.main[0].verification_token : ""
}

output "dkim_tokens" {
  description = "SES DKIM tokens (add as CNAME records in DNS)"
  value       = var.email_domain != "" ? aws_ses_domain_dkim.main[0].dkim_tokens : []
}

output "configuration_set_name" {
  description = "SES configuration set name"
  value       = aws_ses_configuration_set.main.name
}
