#!/bin/sh
# Deploys (or updates) Service Desk on the server prepared by bootstrap.sh.
# Run from your own machine:
#
#   deploy/oci/deploy.sh <server-ip> <image-tag> [web-app-url]
#
#   server-ip    public IP of the server, e.g. 203.0.113.5
#   image-tag    tag published by CI to ghcr.io, e.g. sha-1a2b3c4
#   web-app-url  where the web app is hosted, e.g. https://service-desk.vercel.app
#
# Addresses use sslip.io, a free DNS name that points at the IP inside it:
#   https://api.203-0-113-5.sslip.io     API
#   https://auth.203-0-113-5.sslip.io    Keycloak
#   https://mail.203-0-113-5.sslip.io    demo mailbox
#   https://grafana.203-0-113-5.sslip.io dashboards
#
# Passwords are generated once and kept in ~/.config/service-desk/oci-secrets.yaml.
set -eu

ip="$1"; tag="$2"; web="${3:-}"
ssh_key="${SSH_KEY:-$HOME/.ssh/servicedesk_oci}"
ssh_user="${SSH_USER:-ubuntu}"
dns="$(echo "$ip" | tr . -).sslip.io"
here="$(cd "$(dirname "$0")/../.." && pwd)"
secrets="$HOME/.config/service-desk/oci-secrets.yaml"

if [ ! -f "$secrets" ]; then
  mkdir -p "$(dirname "$secrets")"
  pw() { LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 24; }
  cat > "$secrets" <<EOF
secrets:
  apiDbPassword: $(pw)
  notificationsDbPassword: $(pw)
  keycloakDbPassword: $(pw)
  keycloakAdminPassword: $(pw)
  s3AccessKey: $(pw)
  s3SecretKey: $(pw)
grafanaAdminPassword: $(pw)
EOF
  chmod 600 "$secrets"
  echo "Generated passwords in $secrets"
fi

ssh_run() { ssh -i "$ssh_key" -o StrictHostKeyChecking=accept-new "$ssh_user@$ip" "$@"; }

echo "Copying the Helm chart and monitoring settings to the server"
tar -C "$here/deploy" -czf - helm monitoring | ssh_run 'rm -rf ~/service-desk-deploy && mkdir -p ~/service-desk-deploy && tar -xzf - -C ~/service-desk-deploy'
scp -q -i "$ssh_key" "$secrets" "$ssh_user@$ip:service-desk-deploy/secrets.yaml"

web_flags="--set web.enabled=true --set images.web=ghcr.io/zaitulakmal/service-desk-frontend:$tag"
if [ -n "$web" ]; then
  web_flags="--set web.enabled=false --set webOrigins[0]=$web --set webUrl=$web"
fi

grafana_password=$(awk '/grafanaAdminPassword/ {print $2}' "$secrets")

ssh_run "export KUBECONFIG=/etc/rancher/k3s/k3s.yaml; set -e; cd ~/service-desk-deploy
helm upgrade --install service-desk helm/service-desk -n service-desk --create-namespace \
  -f secrets.yaml \
  --set scheme=https --set tls.enabled=true \
  --set hosts.app=api.$dns --set hosts.auth=auth.$dns --set hosts.mail=mail.$dns \
  --set images.api=ghcr.io/zaitulakmal/service-desk-api:$tag \
  --set images.notification=ghcr.io/zaitulakmal/service-desk-notification-service:$tag \
  --set images.pullPolicy=IfNotPresent \
  --set infrastructure.keycloak.devClient=false \
  $web_flags --wait --timeout 15m
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts --force-update >/dev/null
helm repo add grafana-community https://grafana-community.github.io/helm-charts --force-update >/dev/null
helm upgrade --install prometheus prometheus-community/prometheus --version 29.37.1 \
  -n monitoring --create-namespace -f monitoring/prometheus-values.yaml --wait
helm upgrade --install grafana grafana-community/grafana --version 14.0.1 -n monitoring \
  -f monitoring/grafana-values.yaml \
  --set-file dashboards.default.service-desk.json=monitoring/service-desk-dashboard.json \
  --set adminPassword=$grafana_password \
  --set ingress.hosts[0]=grafana.$dns \
  --set ingress.tls[0].hosts[0]=grafana.$dns --set ingress.tls[0].secretName=grafana-tls \
  --set-string ingress.annotations.cert-manager\\.io/cluster-issuer=letsencrypt --wait
rm -f secrets.yaml"

echo
echo "Live:"
echo "  API       https://api.$dns/api   (needs a login token)"
echo "  Keycloak  https://auth.$dns"
echo "  Mailbox   https://mail.$dns"
echo "  Grafana   https://grafana.$dns"
[ -z "$web" ] && echo "  Web app   https://api.$dns"
