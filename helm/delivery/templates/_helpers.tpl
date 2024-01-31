{{- define "delivery.name" -}}
{{- default .Values.delivery.name "delivery" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "delivery.fullname" -}}
{{- $name := default .Values.delivery.name "delivery" }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

