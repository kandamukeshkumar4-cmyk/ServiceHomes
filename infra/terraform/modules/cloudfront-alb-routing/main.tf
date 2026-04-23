data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}

data "aws_cloudfront_cache_policy" "caching_disabled" {
  name = "Managed-CachingDisabled"
}

resource "aws_cloudfront_origin_request_policy" "websocket" {
  name    = "${var.name}-websocket-origin-request"
  comment = "Forward websocket auth, cookies, and handshake context to the ALB."

  cookies_config {
    cookie_behavior = "all"
  }

  headers_config {
    header_behavior = "whitelist"

    headers {
      items = [
        "Authorization",
        "CloudFront-Forwarded-Proto",
        "Origin",
        "Sec-WebSocket-Extensions",
        "Sec-WebSocket-Key",
        "Sec-WebSocket-Protocol",
        "Sec-WebSocket-Version"
      ]
    }
  }

  query_strings_config {
    query_string_behavior = "all"
  }
}

resource "aws_cloudfront_distribution" "this" {
  enabled             = var.enabled
  aliases             = var.aliases
  comment             = var.comment != null ? var.comment : "${var.name} edge routing"
  default_root_object = var.default_root_object
  http_version        = "http2and3"
  is_ipv6_enabled     = true
  price_class         = var.price_class
  web_acl_id          = var.web_acl_id
  tags                = var.tags

  origin {
    domain_name = var.web_origin_domain_name
    origin_id   = var.web_origin_id

    custom_origin_config {
      http_port                = var.web_origin_http_port
      https_port               = var.web_origin_https_port
      origin_keepalive_timeout = 5
      origin_protocol_policy   = var.web_origin_protocol_policy
      origin_read_timeout      = 30
      origin_ssl_protocols     = ["TLSv1.2"]
    }
  }

  origin {
    domain_name = var.alb_domain_name
    origin_id   = var.alb_origin_id

    custom_origin_config {
      http_port                = var.alb_http_port
      https_port               = var.alb_https_port
      origin_keepalive_timeout = 60
      origin_protocol_policy   = var.alb_origin_protocol_policy
      origin_read_timeout      = var.websocket_origin_read_timeout
      origin_ssl_protocols     = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = data.aws_cloudfront_cache_policy.caching_optimized.id
    compress               = true
    target_origin_id       = var.web_origin_id
    viewer_protocol_policy = "redirect-to-https"
  }

  ordered_cache_behavior {
    path_pattern             = "/ws/*"
    allowed_methods          = ["GET", "HEAD", "OPTIONS"]
    cached_methods           = ["GET", "HEAD"]
    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled.id
    compress                 = false
    origin_request_policy_id = aws_cloudfront_origin_request_policy.websocket.id
    target_origin_id         = var.alb_origin_id
    viewer_protocol_policy   = "redirect-to-https"
  }

  restrictions {
    geo_restriction {
      restriction_type = var.geo_restriction_type
      locations        = var.geo_restriction_locations
    }
  }

  viewer_certificate {
    acm_certificate_arn            = var.acm_certificate_arn
    cloudfront_default_certificate = var.acm_certificate_arn == null
    minimum_protocol_version       = var.acm_certificate_arn == null ? "TLSv1" : "TLSv1.2_2021"
    ssl_support_method             = var.acm_certificate_arn == null ? null : "sni-only"
  }
}
