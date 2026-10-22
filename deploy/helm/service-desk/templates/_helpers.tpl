{{/* Every object is prefixed with the release name, e.g. "service-desk-api". */}}
{{- define "sd.name" -}}
{{ .Release.Name }}
{{- end -}}

{{- define "sd.labels" -}}
app.kubernetes.io/part-of: service-desk
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/*
Addresses of the dependencies. Each one is either the in-cluster component (when
infrastructure.<name>.enabled) or the managed service given under "external".
*/}}
{{- define "sd.dbHost" -}}
{{- if .Values.infrastructure.postgres.enabled -}}
{{ .Release.Name }}-postgres
{{- else -}}
{{ required "external.databaseHost is required when the in-cluster Postgres is off" .Values.external.databaseHost }}
{{- end -}}
{{- end -}}

{{- define "sd.kafka" -}}
{{- if .Values.infrastructure.kafka.enabled -}}
{{ .Release.Name }}-kafka:9092
{{- else -}}
{{ required "external.kafkaBootstrapServers is required when the in-cluster Kafka is off" .Values.external.kafkaBootstrapServers }}
{{- end -}}
{{- end -}}

{{- define "sd.s3Endpoint" -}}
{{- if .Values.infrastructure.rustfs.enabled -}}
http://{{ .Release.Name }}-rustfs:9000
{{- else -}}
{{ .Values.external.s3Endpoint }}
{{- end -}}
{{- end -}}

{{- define "sd.oidcIssuer" -}}
{{ .Values.external.oidcIssuer | default (printf "%s://%s/realms/servicedesk" .Values.scheme .Values.hosts.auth) }}
{{- end -}}

{{- define "sd.jwkSetUri" -}}
{{- if .Values.infrastructure.keycloak.enabled -}}
http://{{ .Release.Name }}-keycloak:8080/realms/servicedesk/protocol/openid-connect/certs
{{- else -}}
{{ include "sd.oidcIssuer" . }}/protocol/openid-connect/certs
{{- end -}}
{{- end -}}

{{- define "sd.smtpHost" -}}
{{- if .Values.infrastructure.mailpit.enabled -}}
{{ .Release.Name }}-mailpit
{{- else -}}
{{ required "external.smtpHost is required when the in-cluster Mailpit is off" .Values.external.smtpHost }}
{{- end -}}
{{- end -}}

{{/* Hardened defaults for our own containers: non-root, read-only filesystem, no extra privileges. */}}
{{- define "sd.podSecurity" -}}
runAsNonRoot: true
seccompProfile:
  type: RuntimeDefault
{{- end -}}

{{- define "sd.containerSecurity" -}}
allowPrivilegeEscalation: false
readOnlyRootFilesystem: true
capabilities:
  drop: ["ALL"]
{{- end -}}
