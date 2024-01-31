{{- define "payment.name" -}}
{{- default .Values.payment.name "payment" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "payment.fullname" -}}
{{- $name := default .Values.payment.name "payment" }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
