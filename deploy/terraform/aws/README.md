# AWS infrastructure

Creates everything Service Desk needs on AWS:

| File | Creates |
|---|---|
| `network.tf` | VPC across 2 availability zones, public and private subnets, one NAT gateway |
| `eks.tf` | EKS cluster in **Auto Mode** (AWS runs and scales the nodes), the API's IAM role for S3 via **Pod Identity** |
| `data.tf` | RDS PostgreSQL 16 (private, encrypted, admin password kept in Secrets Manager), S3 bucket for attachments (private, encrypted, versioned) |
| `ci.tf` | ECR repositories, GitHub OIDC provider and the deploy role GitHub Actions assumes |

## Cost: read this first

This is real infrastructure and **AWS charges for it every hour it exists**: the EKS
control plane, the NAT gateway, the RDS instance, the load balancer and the nodes. Left
running, it adds up to well over USD 100 a month. Estimate it in the
[AWS Pricing Calculator](https://calculator.aws/) before applying, and destroy it after
a demo:

```bash
terraform destroy -var github_repository=OWNER/REPO
```

## Steps

1. Sign in to AWS on your machine (`aws configure sso` or `aws configure`).
2. Create everything (about 15–20 minutes):
   ```bash
   cd deploy/terraform/aws
   terraform init
   terraform apply -var github_repository=OWNER/REPO
   ```
3. Copy the outputs into the GitHub repository's **Settings → Secrets and variables →
   Actions → Variables**: `AWS_REGION`, `EKS_CLUSTER_NAME`, `AWS_DEPLOY_ROLE_ARN`,
   `ECR_REGISTRY`, `RDS_ENDPOINT`, `RDS_MASTER_SECRET_ARN`, `ATTACHMENTS_BUCKET`.
4. Add a domain:
   - Request an ACM certificate for `servicedesk.<your-domain>` and
     `*.servicedesk.<your-domain>`, and save its ARN as the variable `ACM_CERTIFICATE_ARN`.
   - Save the host names as `APP_HOST`, `AUTH_HOST` and `MAIL_HOST`.
   - Add the Keycloak redirect URI for `https://APP_HOST/*` to the `servicedesk-web`
     client in `deploy/helm/service-desk/files/servicedesk-realm.json`.
5. Add the passwords as repository **secrets**: `API_DB_PASSWORD`,
   `NOTIFICATIONS_DB_PASSWORD`, `KEYCLOAK_DB_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD`.
6. Run the **Deploy to AWS** workflow from the Actions tab.
7. Point your DNS records at the load balancer address it prints (`kubectl get ingress`).

## Choices made for a demo budget

- Kafka, Keycloak and Mailpit run inside the cluster. In production they would be
  Amazon MSK, the company's identity provider (e.g. Microsoft Entra ID) and Amazon SES.
- One NAT gateway and a single-AZ `db.t4g.micro` database. Production would use one NAT
  gateway per zone and Multi-AZ RDS.
- `skip_final_snapshot = true` and no deletion protection on RDS, so `terraform destroy`
  is clean. Turn both around for real data.
