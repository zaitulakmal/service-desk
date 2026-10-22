variable "name" {
  description = "Prefix for every resource."
  type        = string
  default     = "service-desk"
}

variable "region" {
  description = "AWS region. ap-southeast-1 is Singapore; ap-southeast-5 (Malaysia) also works if every service used here is available there."
  type        = string
  default     = "ap-southeast-1"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version. Check the versions EKS currently supports before changing."
  type        = string
  default     = "1.35"
}

variable "github_repository" {
  description = "GitHub repository allowed to deploy, as owner/name, e.g. zaitulakmal/service-desk."
  type        = string
}

variable "db_instance_class" {
  description = "RDS instance size. db.t4g.micro is enough for a demo."
  type        = string
  default     = "db.t4g.micro"
}
