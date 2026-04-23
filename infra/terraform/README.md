# AWS Infrastructure with Terraform

ServiceHomes production infrastructure provisioned via Terraform. This directory contains all infrastructure-as-code for AWS resources in staging and production environments.

## Directory Structure

```
infra/terraform/
├── main.tf                    # Provider, backend, and module instantiation
├── variables.tf               # Root-level variable definitions
├── outputs.tf                 # Root-level outputs
├── environments/
│   ├── staging.tfvars         # Staging-specific variable values
│   └── production.tfvars      # Production-specific variable values
└── modules/
    ├── vpc/                   # VPC, subnets, NAT, route tables
    ├── rds/                   # PostgreSQL RDS instance
    ├── ecs/                   # ECS/Fargate cluster and service
    ├── alb/                   # Application Load Balancer
    ├── ecr/                   # ECR container registries
    ├── frontend/              # S3 + CloudFront for Angular SPA
    ├── iam/                   # IAM roles and policies
    └── ses/                   # SES email configuration
```

## Bootstrap (One-Time Setup)

Before running Terraform, create the remote state backend:

```bash
# Create S3 bucket for state
aws s3api create-bucket \
  --bucket servicehomes-terraform-state \
  --region us-east-1

# Create DynamoDB table for state locking
aws dynamodb create-table \
  --table-name servicehomes-terraform-lock \
  --attribute-definitions AttributeName=LockId,AttributeType=S \
  --key-schema AttributeName=LockId,KeyType=KEY \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

Then uncomment the `backend "s3"` block in `main.tf`.

## Usage

### Initialize

```bash
cd infra/terraform
terraform init
```

### Plan

```bash
# Staging
terraform plan -var-file=environments/staging.tfvars

# Production
terraform plan -var-file=environments/production.tfvars
```

### Apply

```bash
# Staging
terraform apply -var-file=environments/staging.tfvars

# Production
terraform apply -var-file=environments/production.tfvars
```

### Retrieve RDS Password

After apply, retrieve the database credentials from Secrets Manager:

```bash
aws secretsmanager get-secret-value \
  --secret-id servicehomes-staging-db-credentials \
  --region us-east-1 \
  --query SecretString \
  --output text | jq
```

### Invalidate CloudFront Cache

After deploying new frontend assets:

```bash
aws cloudfront create-invalidation \
  --distribution-id $(terraform output -raw cloudfront_distribution_id) \
  --paths "/*"
```

### Push Image to ECR

```bash
# Login
aws ecr get-login-password --region us-east-1 | docker login \
  --username AWS \
  --password-stdin $(aws sts get-caller-identity --query Account --output text).dkr.ecr.us-east-1.amazonaws.com

# Tag and push
docker tag servicehomes-api:latest $(terraform output -raw ecr_api_repository_url):latest
docker push $(terraform output -raw ecr_api_repository_url):latest
```

## SES Domain Verification

After applying, SES domain identity requires manual DNS verification:

1. Retrieve verification token: `terraform output -raw ses_verification_token`
2. Add TXT record to your DNS: `_amazonses.<domain> TXT "<token>"`
3. Retrieve DKIM tokens: `terraform output -json ses_dkim_tokens`
4. Add three CNAME records for DKIM verification

Domain verification typically takes a few minutes to complete.

## Cost Control

| Resource | Staging | Production |
|----------|---------|------------|
| RDS | db.t3.micro | db.t3.medium |
| ECS Task | 256 CPU / 512 MB | 512 CPU / 1024 MB |
| ECS Desired Count | 1 | 2 |
| RDS Multi-AZ | No | Yes |
| RDS Deletion Protection | No | Yes |
| ALB Deletion Protection | No | Yes |
