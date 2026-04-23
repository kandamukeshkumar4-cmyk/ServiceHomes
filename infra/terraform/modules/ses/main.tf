resource "aws_ses_domain_identity" "main" {
  count  = var.email_domain != "" ? 1 : 0
  domain = var.email_domain
}

resource "aws_ses_domain_dkim" "main" {
  count  = var.email_domain != "" ? 1 : 0
  domain = aws_ses_domain_identity.main[0].domain
}

resource "aws_ses_configuration_set" "main" {
  name = "${var.project_name}-${var.environment}-email-events"

  delivery_options {
    tls_policy = "Require"
  }

  tracking_options {
    custom_redirect_domain = var.email_domain != "" ? var.email_domain : null
  }
}

resource "aws_ses_event_destination" "cloudwatch" {
  count                  = var.email_domain != "" ? 1 : 0
  name                   = "${var.project_name}-${var.environment}-email-cloudwatch"
  configuration_set_name = aws_ses_configuration_set.main.name

  enabled = true

  matching_types = [
    "send",
    "reject",
    "bounce",
    "complaint",
    "delivery",
    "open",
    "click"
  ]

  cloudwatch_destination {
    default_value  = "email-event"
    dimension_name = "MessageTag"
    value_source   = "messageTag"
  }
}
