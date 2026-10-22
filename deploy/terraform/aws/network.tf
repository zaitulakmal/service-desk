data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, 2)
}

# Two availability zones. Public subnets hold the load balancer; private subnets hold
# the cluster nodes and the database, which are never reachable from the internet.
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 6.7"

  name = var.name
  cidr = "10.20.0.0/16"
  azs  = local.azs

  public_subnets  = ["10.20.0.0/20", "10.20.16.0/20"]
  private_subnets = ["10.20.64.0/18", "10.20.128.0/18"]

  # One NAT gateway instead of one per zone: cheaper, fine for a demo.
  enable_nat_gateway = true
  single_nat_gateway = true

  # Lets the load balancer controller find the right subnets.
  public_subnet_tags  = { "kubernetes.io/role/elb" = 1 }
  private_subnet_tags = { "kubernetes.io/role/internal-elb" = 1 }
}
