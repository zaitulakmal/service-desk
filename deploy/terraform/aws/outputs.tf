# Copy these into the GitHub repository variables used by .github/workflows/deploy-aws.yml.

output "AWS_REGION" {
  value = var.region
}

output "EKS_CLUSTER_NAME" {
  value = module.eks.cluster_name
}

output "AWS_DEPLOY_ROLE_ARN" {
  value = aws_iam_role.github_deploy.arn
}

output "ECR_REGISTRY" {
  value = split("/", aws_ecr_repository.service["api"].repository_url)[0]
}

output "RDS_ENDPOINT" {
  value = aws_db_instance.this.address
}

output "RDS_MASTER_SECRET_ARN" {
  value = aws_db_instance.this.master_user_secret[0].secret_arn
}

output "ATTACHMENTS_BUCKET" {
  value = aws_s3_bucket.attachments.bucket
}
