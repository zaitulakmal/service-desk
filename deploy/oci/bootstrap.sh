#!/bin/sh
# One-time setup of a fresh Ubuntu 24.04 server (e.g. Oracle Cloud Always Free, Ampere A1):
# a single-node Kubernetes (k3s, which includes the Traefik ingress), Helm, and
# cert-manager issuing free Let's Encrypt certificates.
#
# Run on the server:  sudo sh bootstrap.sh
set -eu

K3S_VERSION="v1.37.1+k3s1"
CERT_MANAGER_VERSION="v1.21.2"

# Oracle's Ubuntu images reject all traffic except SSH with iptables rules of their own,
# including forwarded traffic, which Kubernetes needs. The cloud security list (ports
# 22, 80, 443) stays the firewall.
iptables -D INPUT -j REJECT --reject-with icmp-host-prohibited 2>/dev/null || true
iptables -D FORWARD -j REJECT --reject-with icmp-host-prohibited 2>/dev/null || true
if command -v netfilter-persistent >/dev/null; then netfilter-persistent save; fi

if ! command -v k3s >/dev/null; then
  curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION="$K3S_VERSION" sh -s - --write-kubeconfig-mode 644
fi
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
until kubectl get nodes 2>/dev/null | grep -q ' Ready'; do sleep 3; done

if ! command -v helm >/dev/null; then
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
fi

helm repo add jetstack https://charts.jetstack.io --force-update
helm upgrade --install cert-manager jetstack/cert-manager --version "$CERT_MANAGER_VERSION" \
  --namespace cert-manager --create-namespace --set crds.enabled=true --wait

kubectl apply -f - <<'EOF'
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    privateKeySecretRef:
      name: letsencrypt-account
    solvers:
      - http01:
          ingress:
            ingressClassName: traefik
EOF

echo "Server ready: k3s $(k3s --version | head -1 | awk '{print $3}'), cert-manager $CERT_MANAGER_VERSION"
