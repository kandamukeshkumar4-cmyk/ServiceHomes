# ADR-007: AWS Infrastructure with Terraform

**Status:** Accepted  
**Date:** 2026-04-23  
**Context:** ServiceHomes production infrastructure provisioning

## Decision

Use HashiCorp Terraform to provision all AWS infrastructure resources for ServiceHomes, organized as reusable modules with environment-specific variable files.

## Alternatives Considered

### AWS CDK
- **Pros:** TypeScript/Python familiarity, programmatic constructs, tight AWS integration
- **Cons:** Locks team into AWS ecosystem, harder to audit resource changes, state management less transparent
- **Rejected because:** We want cloud-agnostic IaC that produces readable, auditable HCL files. CDK's synthesized CloudFormation templates are harder to review in PRs.

### CloudFormation
- **Pros:** Native AWS support, no additional tooling
- **Cons:** Verbose YAML/JSON, limited modularity, no ecosystem of third-party modules
- **Rejected because:** Terraform's module ecosystem, plan output readability, and multi-cloud capability outweigh native AWS integration.

### Pulumi
- **Pros:** General-purpose languages, strong typing
- **Cons:** Requires language runtime, steeper learning curve for ops team
- **Rejected because:** HCL is purpose-built for infrastructure and easier for the whole team to review.

## Architecture

### Module Structure

Each AWS service area is isolated into its own Terraform module:

| Module | Purpose |
|--------|---------|
| `vpc` | Network topology: VPC, subnets, NAT, route tables |
| `rds` | PostgreSQL database with Secrets Manager integration |
| `ecs` | Fargate cluster, task definition, service deployment |
| `alb` | Application Load Balancer with TLS termination |
| `ecr` | Container registries with lifecycle policies |
| `frontend` | S3 static hosting + CloudFront CDN |
| `iam` | Least-privilege IAM roles and policies |
| `ses` | Email sending with domain verification |

### Environment Strategy

We use separate `.tfvars` files per environment rather than Terraform workspaces:

- `environments/staging.tfvars` — small instances, single task, no Multi-AZ
- `environments/production.tfvars` — larger instances, multiple tasks, Multi-AZ RDS

**Why tfvars over workspaces:**
1. Each environment's configuration is explicit and reviewable in Git
2. No risk of accidentally applying staging config to production workspace
3. Easier to add new environments (e.g., `dev`, `qa`) by copying a tfvars file
4. CI/CD pipelines can target specific environments with `-var-file` flags

### State Management

Remote state stored in S3 with DynamoDB locking:
- Bucket: `servicehomes-terraform-state`
- Table: `servicehomes-terraform-lock`
- Each environment uses a separate state key path

## Cost Control

Staging uses minimal resources to keep costs low:

| Resource | Staging | Production |
|----------|---------|------------|
| RDS instance | db.t3.micro (~$12/mo) | db.t3.medium (~$49/mo) |
| ECS task | 256 CPU / 512 MB | 512 CPU / 1024 MB |
| ECS count | 1 | 2 |
| RDS Multi-AZ | Disabled | Enabled |
| NAT Gateways | 2 (required for HA) | 2 |

Estimated monthly cost:
- Staging: ~$80-100
- Production: ~$200-300

## Security

- RDS credentials stored in Secrets Manager, never in state or variables
- ECS tasks use IAM roles with least-privilege policies
- S3 frontend bucket blocks all public access, served via CloudFront OAC
- RDS security group only allows ingress from ECS security group
- Production has deletion protection enabled on RDS and ALB
- All storage encrypted at rest (RDS, ECR, S3)

## Consequences

### Positive
- Infrastructure changes are version-controlled and reviewable
- `terraform plan` provides safe preview of changes
- Module reuse between staging and production reduces duplication
- Easy to add new environments or regions

### Negative
- Requires Terraform knowledge across the team
- State file must be carefully managed (mitigated by remote backend)
- Initial bootstrap requires manual S3/DynamoDB creation

## Related

- Epic: AWS Production Infrastructure (Terraform)
- Branch: `feat/aws-terraform-infrastructure`
- Docker Compose files in `infra/docker/` for local development
